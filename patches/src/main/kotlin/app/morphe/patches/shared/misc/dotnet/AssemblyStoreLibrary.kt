/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ELF_CLASS = 4
private const val ELF_CLASS_64 = 2
private const val ELF64_SECTION_TABLE = 0x28
private const val ELF64_SECTION_ENTRY_SIZE = 0x3a
private const val ELF32_SECTION_TABLE = 0x20
private const val ELF32_SECTION_ENTRY_SIZE = 0x2e
private const val ELF64_SECTION_OFFSET = 24
private const val ELF64_SECTION_SIZE = 32
private const val ELF32_SECTION_OFFSET = 16
private const val ELF32_SECTION_SIZE = 20
private const val ELF_SECTION_TYPE = 4
private const val ELF_SECTION_NO_BITS = 8
private const val ELF_SECTION_TABLE_ALIGNMENT = 8
private const val PAYLOAD_SECTION = "payload"

private const val STORE_MAGIC = 0x41424158
private const val STORE_HEADER_SIZE = 20
private const val STORE_ENTRY_COUNT = 8
private const val STORE_INDEX_SIZE = 16
private const val DESCRIPTOR_SIZE = 28
private const val DESCRIPTOR_DATA_OFFSET = 4
private const val DESCRIPTOR_DATA_SIZE = 8
private const val ASSEMBLY_ALIGNMENT = 16

private const val LZ4_ASSEMBLY_MAGIC = 0x5a4c4158
private const val LZ4_ASSEMBLY_HEADER_SIZE = 12
private const val LZ4_ASSEMBLY_DECOMPRESSED_SIZE = 8

internal class AssemblyStoreLibrary(private val elf: ByteArray) {
    private val elfBuffer = ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN)
    private val is64Bit = elf[ELF_CLASS].toInt() == ELF_CLASS_64
    private val sectionTableOffset =
        if (is64Bit) elfBuffer.getLong(ELF64_SECTION_TABLE).toInt() else elfBuffer.getInt(ELF32_SECTION_TABLE)
    private val sectionEntrySizeField = if (is64Bit) ELF64_SECTION_ENTRY_SIZE else ELF32_SECTION_ENTRY_SIZE
    private val sectionEntrySize = elfBuffer.getShort(sectionEntrySizeField).toInt()
    private val sectionCount = elfBuffer.getShort(sectionEntrySizeField + Short.SIZE_BYTES).toInt()
    private val sectionNamesSection = elfBuffer.getShort(sectionEntrySizeField + 2 * Short.SIZE_BYTES).toInt()
    private val payloadSection = (0 until sectionCount).firstOrNull { sectionName(it) == PAYLOAD_SECTION }
        ?: throw PatchException("Assembly store library has no $PAYLOAD_SECTION section")
    private val payloadOffset = sectionOffset(payloadSection)
    private val payloadSize = sectionSize(payloadSection)
    private val store = ByteBuffer.wrap(elf, payloadOffset, payloadSize).slice().order(ByteOrder.LITTLE_ENDIAN).also {
        if (it.getInt(0) != STORE_MAGIC) throw PatchException("$PAYLOAD_SECTION section is not an assembly store")
    }
    private val entryCount = store.getInt(STORE_ENTRY_COUNT)
    private val descriptorsOffset = STORE_HEADER_SIZE + store.getInt(STORE_INDEX_SIZE)
    private val assemblyNames = readAssemblyNames()

    fun readAssembly(name: String): ByteArray {
        val descriptor = descriptorOffset(name)
        val dataOffset = store.getInt(descriptor + DESCRIPTOR_DATA_OFFSET)
        val dataSize = store.getInt(descriptor + DESCRIPTOR_DATA_SIZE)
        val start = payloadOffset + dataOffset
        if (store.getInt(dataOffset) != LZ4_ASSEMBLY_MAGIC) return elf.copyOfRange(start, start + dataSize)

        val decompressedSize = store.getInt(dataOffset + LZ4_ASSEMBLY_DECOMPRESSED_SIZE)
        return decompressLz4Block(elf, start + LZ4_ASSEMBLY_HEADER_SIZE, start + dataSize, decompressedSize)
    }

    fun writeWithAssembly(name: String, image: ByteArray, destination: File) {
        val sectionTableSize = sectionCount * sectionEntrySize
        val payloadEnd = payloadOffset + payloadSize
        if (sectionTableOffset < payloadEnd || sectionTableOffset + sectionTableSize != elf.size) {
            throw PatchException("Assembly store library does not end with its $PAYLOAD_SECTION section")
        }
        val sectionsAfterPayload = (0 until sectionCount).filter {
            it != payloadSection && !isNoBits(it) && sectionOffset(it) + sectionSize(it) > payloadOffset
        }
        if (sectionsAfterPayload.isNotEmpty()) {
            throw PatchException("Assembly store library has sections $sectionsAfterPayload after its $PAYLOAD_SECTION section")
        }

        val imageOffset = align(payloadSize, ASSEMBLY_ALIGNMENT)
        val newPayloadSize = imageOffset + image.size
        val newSectionTableOffset = align(payloadOffset + newPayloadSize, ELF_SECTION_TABLE_ALIGNMENT)

        val head = elf.copyOf(payloadEnd)
        val headBuffer = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
        val descriptor = payloadOffset + descriptorOffset(name)
        headBuffer.putInt(descriptor + DESCRIPTOR_DATA_OFFSET, imageOffset)
        headBuffer.putInt(descriptor + DESCRIPTOR_DATA_SIZE, image.size)

        val sectionTable = elf.copyOfRange(sectionTableOffset, sectionTableOffset + sectionTableSize)
        val sectionTableBuffer = ByteBuffer.wrap(sectionTable).order(ByteOrder.LITTLE_ENDIAN)
        val payloadHeader = payloadSection * sectionEntrySize
        if (is64Bit) {
            headBuffer.putLong(ELF64_SECTION_TABLE, newSectionTableOffset.toLong())
            sectionTableBuffer.putLong(payloadHeader + ELF64_SECTION_SIZE, newPayloadSize.toLong())
        } else {
            headBuffer.putInt(ELF32_SECTION_TABLE, newSectionTableOffset)
            sectionTableBuffer.putInt(payloadHeader + ELF32_SECTION_SIZE, newPayloadSize)
        }

        destination.outputStream().buffered().use { output ->
            output.write(head)
            output.write(ByteArray(imageOffset - payloadSize))
            output.write(image)
            output.write(ByteArray(newSectionTableOffset - payloadOffset - newPayloadSize))
            output.write(sectionTable)
        }
    }

    private fun descriptorOffset(name: String): Int {
        val index = assemblyNames.indexOf(name)
        if (index < 0) throw PatchException("Assembly store has no $name")
        return descriptorsOffset + index * DESCRIPTOR_SIZE
    }

    private fun readAssemblyNames(): List<String> {
        var offset = descriptorsOffset + entryCount * DESCRIPTOR_SIZE
        return List(entryCount) {
            val length = store.getInt(offset)
            val name = String(elf, payloadOffset + offset + Int.SIZE_BYTES, length, Charsets.UTF_8)
            offset += Int.SIZE_BYTES + length
            name
        }
    }

    private fun sectionHeader(section: Int) = sectionTableOffset + section * sectionEntrySize

    private fun isNoBits(section: Int) = elfBuffer.getInt(sectionHeader(section) + ELF_SECTION_TYPE) == ELF_SECTION_NO_BITS

    private fun sectionOffset(section: Int) = if (is64Bit) {
        elfBuffer.getLong(sectionHeader(section) + ELF64_SECTION_OFFSET).toInt()
    } else {
        elfBuffer.getInt(sectionHeader(section) + ELF32_SECTION_OFFSET)
    }

    private fun sectionSize(section: Int) = if (is64Bit) {
        elfBuffer.getLong(sectionHeader(section) + ELF64_SECTION_SIZE).toInt()
    } else {
        elfBuffer.getInt(sectionHeader(section) + ELF32_SECTION_SIZE)
    }

    private fun sectionName(section: Int): String {
        val start = sectionOffset(sectionNamesSection) + elfBuffer.getInt(sectionHeader(section))
        var end = start
        while (elf[end] != 0.toByte()) end++
        return String(elf, start, end - start, Charsets.US_ASCII)
    }

    private fun align(value: Int, alignment: Int) = (value + alignment - 1) / alignment * alignment
}
