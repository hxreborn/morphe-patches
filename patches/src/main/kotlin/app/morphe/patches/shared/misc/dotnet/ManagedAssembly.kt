/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

private const val PE_HEADER_POINTER = 0x3c
private const val PE_SIGNATURE = 0x00004550
private const val COFF_SECTION_COUNT = 6
private const val COFF_OPTIONAL_HEADER_SIZE = 20
private const val COFF_HEADER_SIZE = 24
private const val OPTIONAL_HEADER_PE32_PLUS = 0x20b
private const val PE32_DATA_DIRECTORIES = 96
private const val PE32_PLUS_DATA_DIRECTORIES = 112
private const val CLI_HEADER_DIRECTORY = 14
private const val DATA_DIRECTORY_SIZE = 8
private const val SECTION_HEADER_SIZE = 40
private const val SECTION_VIRTUAL_SIZE = 8
private const val SECTION_VIRTUAL_ADDRESS = 12
private const val SECTION_RAW_DATA = 20
private const val CLI_METADATA_DIRECTORY = 8

private const val METADATA_SIGNATURE = 0x424a5342
private const val METADATA_VERSION_LENGTH = 12
private const val METADATA_VERSION = 16
private const val STREAM_COUNT_AFTER_VERSION = 2
private const val STREAM_HEADERS_AFTER_VERSION = 4
private const val STREAM_HEADER_NAME = 8
private const val STREAM_NAME_ALIGNMENT = 4
private const val TABLES_STREAM = "#~"
private const val STRINGS_STREAM = "#Strings"
private const val BLOB_STREAM = "#Blob"
private const val TABLES_HEAP_SIZES = 6
private const val TABLES_PRESENT = 8
private const val TABLES_ROW_COUNTS = 24
private const val LARGE_STRING_HEAP = 1
private const val LARGE_GUID_HEAP = 2
private const val LARGE_BLOB_HEAP = 4
private const val TABLE_COUNT = 64
private const val SMALL_INDEX_ROWS = 1 shl 16

private const val MODULE = 0x00
private const val TYPE_REF = 0x01
private const val TYPE_DEF = 0x02
private const val FIELD_POINTER = 0x03
private const val FIELD = 0x04
private const val METHOD_POINTER = 0x05
private const val METHOD_DEF = 0x06
private const val PARAM_POINTER = 0x07
private const val PARAM = 0x08
private const val INTERFACE_IMPL = 0x09
private const val MEMBER_REF = 0x0a
private const val MODULE_REF = 0x1a
private const val TYPE_SPEC = 0x1b
private const val ASSEMBLY_REF = 0x23
private const val RESOLUTION_SCOPE_TAG_BITS = 2
private const val TYPE_DEF_OR_REF_TAG_BITS = 2
private const val MEMBER_REF_PARENT_TAG_BITS = 3
private const val MEMBER_REF_PARENT_TYPE_REF = 1
private const val MEMBER_REF_TOKEN = 0x0a000000
private const val FIELD_SIGNATURE = 0x06

private const val METHOD_HEADER_FORMAT_MASK = 3
private const val TINY_METHOD_HEADER = 2
private const val FAT_METHOD_HEADER = 3
private const val TINY_CODE_SIZE_SHIFT = 2
private const val TINY_MAX_CODE_SIZE = 63
private const val FAT_CODE_SIZE = 4
private const val FAT_HEADER_SIZE = 12

private const val BLOB_ONE_BYTE_LENGTH_MASK = 0x80
private const val BLOB_TWO_BYTE_LENGTH_MASK = 0xc0
private const val BLOB_TWO_BYTE_LENGTH = 0x80

private class PeSection(val virtualAddress: Int, val virtualSize: Int, val rawDataOffset: Int)

internal class ManagedAssembly(val image: ByteArray) {
    private val buffer = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN)
    private val peHeader = buffer.getInt(PE_HEADER_POINTER).also {
        if (buffer.getInt(it) != PE_SIGNATURE) throw PatchException("Assembly is not a PE image")
    }
    private val sections = readSections()
    private val metadata = fileOffset(buffer.getInt(cliHeaderOffset() + CLI_METADATA_DIRECTORY))
    private val streams = readStreams()
    private val tables = stream(TABLES_STREAM)
    private val strings = stream(STRINGS_STREAM)
    private val blobs = stream(BLOB_STREAM)
    private val heapSizes = image[tables + TABLES_HEAP_SIZES].toInt()
    private val rowCounts = readRowCounts()

    private val stringIndex = heapIndexSize(LARGE_STRING_HEAP)
    private val guidIndex = heapIndexSize(LARGE_GUID_HEAP)
    private val blobIndex = heapIndexSize(LARGE_BLOB_HEAP)
    private val fieldIndex = tableIndexSize(FIELD)
    private val methodDefIndex = tableIndexSize(METHOD_DEF)
    private val resolutionScopeIndex = codedIndexSize(RESOLUTION_SCOPE_TAG_BITS, MODULE, MODULE_REF, ASSEMBLY_REF, TYPE_REF)
    private val typeDefOrRefIndex = codedIndexSize(TYPE_DEF_OR_REF_TAG_BITS, TYPE_DEF, TYPE_REF, TYPE_SPEC)
    private val memberRefParentIndex =
        codedIndexSize(MEMBER_REF_PARENT_TAG_BITS, TYPE_DEF, TYPE_REF, MODULE_REF, METHOD_DEF, TYPE_SPEC)

    private val typeRefNameColumn = resolutionScopeIndex
    private val typeRefNamespaceColumn = typeRefNameColumn + stringIndex
    private val typeDefNameColumn = Int.SIZE_BYTES
    private val typeDefNamespaceColumn = typeDefNameColumn + stringIndex
    private val typeDefMethodListColumn = typeDefNamespaceColumn + stringIndex + typeDefOrRefIndex + fieldIndex
    private val methodDefRvaColumn = 0
    private val methodDefNameColumn = Int.SIZE_BYTES + 2 * Short.SIZE_BYTES
    private val memberRefParentColumn = 0
    private val memberRefNameColumn = memberRefParentColumn + memberRefParentIndex
    private val memberRefSignatureColumn = memberRefNameColumn + stringIndex

    private val rowSizes = mapOf(
        MODULE to Short.SIZE_BYTES + stringIndex + 3 * guidIndex,
        TYPE_REF to typeRefNamespaceColumn + stringIndex,
        TYPE_DEF to typeDefMethodListColumn + methodDefIndex,
        FIELD to Short.SIZE_BYTES + stringIndex + blobIndex,
        METHOD_DEF to methodDefNameColumn + stringIndex + blobIndex + tableIndexSize(PARAM),
        PARAM to 2 * Short.SIZE_BYTES + stringIndex,
        INTERFACE_IMPL to tableIndexSize(TYPE_DEF) + typeDefOrRefIndex,
        MEMBER_REF to memberRefSignatureColumn + blobIndex,
    )
    private val tableOffsets = readTableOffsets()

    fun replaceMethodBody(namespace: String, type: String, method: String, code: ByteArray) {
        val body = fileOffset(readColumn(METHOD_DEF, methodRow(namespace, type, method), methodDefRvaColumn, Int.SIZE_BYTES))
        val header = image[body].toInt() and 0xff
        val bodySize = when (header and METHOD_HEADER_FORMAT_MASK) {
            TINY_METHOD_HEADER -> 1 + (header ushr TINY_CODE_SIZE_SHIFT)
            FAT_METHOD_HEADER -> FAT_HEADER_SIZE + buffer.getInt(body + FAT_CODE_SIZE)
            else -> throw PatchException("$type.$method has an unknown method header 0x${header.toString(16)}")
        }
        if (code.size > TINY_MAX_CODE_SIZE) {
            throw PatchException("Replacement code for $type.$method is ${code.size} bytes, a tiny body holds at most $TINY_MAX_CODE_SIZE")
        }
        if (1 + code.size > bodySize) {
            throw PatchException("$type.$method body of $bodySize bytes cannot hold ${code.size} bytes of code")
        }

        image[body] = ((code.size shl TINY_CODE_SIZE_SHIFT) or TINY_METHOD_HEADER).toByte()
        code.copyInto(image, body + 1)
    }

    fun fieldReferenceToken(namespace: String, type: String, field: String): Int {
        val rows = (1..rowCounts[MEMBER_REF]).filter { row ->
            val parent = readColumn(MEMBER_REF, row, memberRefParentColumn, memberRefParentIndex)
            val parentTag = parent and ((1 shl MEMBER_REF_PARENT_TAG_BITS) - 1)
            parentTag == MEMBER_REF_PARENT_TYPE_REF &&
                string(readColumn(MEMBER_REF, row, memberRefNameColumn, stringIndex)) == field &&
                blobFirstByte(readColumn(MEMBER_REF, row, memberRefSignatureColumn, blobIndex)) == FIELD_SIGNATURE &&
                isTypeRefNamed(parent ushr MEMBER_REF_PARENT_TAG_BITS, namespace, type)
        }
        val row = rows.singleOrNull()
            ?: throw PatchException("Expected one reference to $namespace.$type.$field, found ${rows.size}")
        return MEMBER_REF_TOKEN or row
    }

    private fun methodRow(namespace: String, type: String, method: String): Int {
        val typeRows = (1..rowCounts[TYPE_DEF]).filter { row ->
            string(readColumn(TYPE_DEF, row, typeDefNameColumn, stringIndex)) == type &&
                string(readColumn(TYPE_DEF, row, typeDefNamespaceColumn, stringIndex)) == namespace
        }
        val typeRow = typeRows.singleOrNull()
            ?: throw PatchException("Expected one type $namespace.$type, found ${typeRows.size}")

        val firstMethod = readColumn(TYPE_DEF, typeRow, typeDefMethodListColumn, methodDefIndex)
        val endMethod = if (typeRow < rowCounts[TYPE_DEF]) {
            readColumn(TYPE_DEF, typeRow + 1, typeDefMethodListColumn, methodDefIndex)
        } else {
            rowCounts[METHOD_DEF] + 1
        }
        val methodRows = (firstMethod until endMethod).filter { row ->
            string(readColumn(METHOD_DEF, row, methodDefNameColumn, stringIndex)) == method
        }
        return methodRows.singleOrNull()
            ?: throw PatchException("Expected one method $type.$method, found ${methodRows.size}")
    }

    private fun isTypeRefNamed(row: Int, namespace: String, type: String) =
        string(readColumn(TYPE_REF, row, typeRefNameColumn, stringIndex)) == type &&
            string(readColumn(TYPE_REF, row, typeRefNamespaceColumn, stringIndex)) == namespace

    private fun readColumn(table: Int, row: Int, column: Int, size: Int): Int {
        val offset = tableOffsets.getValue(table) + (row - 1) * rowSizes.getValue(table) + column
        return if (size == Short.SIZE_BYTES) buffer.getShort(offset).toInt() and 0xffff else buffer.getInt(offset)
    }

    private fun string(index: Int) = nullTerminatedString(strings + index, Charsets.UTF_8)

    private fun nullTerminatedString(start: Int, charset: Charset): String {
        var end = start
        while (image[end] != 0.toByte()) end++
        return String(image, start, end - start, charset)
    }

    private fun blobFirstByte(index: Int): Int {
        val start = blobs + index
        val lead = image[start].toInt() and 0xff
        val lengthSize = when {
            lead and BLOB_ONE_BYTE_LENGTH_MASK == 0 -> Byte.SIZE_BYTES
            lead and BLOB_TWO_BYTE_LENGTH_MASK == BLOB_TWO_BYTE_LENGTH -> Short.SIZE_BYTES
            else -> Int.SIZE_BYTES
        }
        return image[start + lengthSize].toInt() and 0xff
    }

    private fun heapIndexSize(largeHeapFlag: Int) =
        if (heapSizes and largeHeapFlag != 0) Int.SIZE_BYTES else Short.SIZE_BYTES

    private fun tableIndexSize(table: Int) =
        if (rowCounts[table] < SMALL_INDEX_ROWS) Short.SIZE_BYTES else Int.SIZE_BYTES

    private fun codedIndexSize(tagBits: Int, vararg tables: Int) =
        if (tables.maxOf { rowCounts[it] } < 1 shl (Short.SIZE_BITS - tagBits)) Short.SIZE_BYTES else Int.SIZE_BYTES

    private fun readRowCounts(): IntArray {
        val presentTables = buffer.getLong(tables + TABLES_PRESENT)
        val counts = IntArray(TABLE_COUNT)
        var offset = tables + TABLES_ROW_COUNTS
        for (table in 0 until TABLE_COUNT) {
            if (presentTables and (1L shl table) != 0L) {
                counts[table] = buffer.getInt(offset)
                offset += Int.SIZE_BYTES
            }
        }
        listOf(FIELD_POINTER, METHOD_POINTER, PARAM_POINTER).forEach { table ->
            if (counts[table] != 0) throw PatchException("Unsupported metadata pointer table 0x${table.toString(16)} with ${counts[table]} rows")
        }
        return counts
    }

    private fun readTableOffsets(): Map<Int, Int> {
        var offset = tables + TABLES_ROW_COUNTS + rowCounts.count { it != 0 } * Int.SIZE_BYTES
        return rowSizes.keys.sorted().associateWith { table ->
            offset.also { offset += rowCounts[table] * rowSizes.getValue(table) }
        }
    }

    private fun stream(name: String) = streams[name] ?: throw PatchException("Metadata has no $name stream")

    private fun readStreams(): Map<String, Int> {
        if (buffer.getInt(metadata) != METADATA_SIGNATURE) throw PatchException("Assembly has no CLI metadata")
        val versionEnd = metadata + METADATA_VERSION + buffer.getInt(metadata + METADATA_VERSION_LENGTH)
        val count = buffer.getShort(versionEnd + STREAM_COUNT_AFTER_VERSION).toInt()
        var header = versionEnd + STREAM_HEADERS_AFTER_VERSION
        return List(count) {
            val streamOffset = buffer.getInt(header)
            val name = nullTerminatedString(header + STREAM_HEADER_NAME, Charsets.US_ASCII)
            val nameEnd = header + STREAM_HEADER_NAME + name.length + 1
            header = (nameEnd + STREAM_NAME_ALIGNMENT - 1) / STREAM_NAME_ALIGNMENT * STREAM_NAME_ALIGNMENT
            name to metadata + streamOffset
        }.toMap()
    }

    private fun cliHeaderOffset(): Int {
        val optionalHeader = peHeader + COFF_HEADER_SIZE
        val dataDirectories = if (buffer.getShort(optionalHeader).toInt() == OPTIONAL_HEADER_PE32_PLUS) {
            PE32_PLUS_DATA_DIRECTORIES
        } else {
            PE32_DATA_DIRECTORIES
        }
        return fileOffset(buffer.getInt(optionalHeader + dataDirectories + CLI_HEADER_DIRECTORY * DATA_DIRECTORY_SIZE))
    }

    private fun readSections(): List<PeSection> {
        val count = buffer.getShort(peHeader + COFF_SECTION_COUNT).toInt()
        val optionalHeaderSize = buffer.getShort(peHeader + COFF_OPTIONAL_HEADER_SIZE).toInt() and 0xffff
        val first = peHeader + COFF_HEADER_SIZE + optionalHeaderSize
        return List(count) {
            val header = first + it * SECTION_HEADER_SIZE
            PeSection(
                virtualAddress = buffer.getInt(header + SECTION_VIRTUAL_ADDRESS),
                virtualSize = buffer.getInt(header + SECTION_VIRTUAL_SIZE),
                rawDataOffset = buffer.getInt(header + SECTION_RAW_DATA),
            )
        }
    }

    private fun fileOffset(rva: Int): Int {
        val section = sections.firstOrNull { rva >= it.virtualAddress && rva < it.virtualAddress + it.virtualSize }
            ?: throw PatchException("RVA 0x${rva.toString(16)} is outside every PE section")
        return rva - section.virtualAddress + section.rawDataOffset
    }
}
