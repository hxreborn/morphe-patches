/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException

private const val FILE_SIZE_OFFSET = 32
private const val MAP_OFF_OFFSET = 52
private const val STRING_IDS_OFFSET = 56
private const val TYPE_IDS_OFFSET = 64
private const val PROTO_IDS_OFFSET = 72
private const val FIELD_IDS_OFFSET = 80
private const val METHOD_IDS_OFFSET = 88
private const val CLASS_DEFS_OFFSET = 96
private const val DATA_OFFSET = 104

private const val HEADER_SIZE_OFFSET = 36
private const val ENDIAN_TAG_OFFSET = 40

private const val DEX_HEADER_SIZE = 112
private const val ENDIAN_TAG = 0x12345678
private const val CLASS_DEF_SIZE = 32
private const val CLASS_DATA_FIELD = 24
private const val FIELD_ID_SIZE = 8
private const val METHOD_ID_SIZE = 8
private const val PROTO_ID_SIZE = 12
private const val MAP_ENTRY_SIZE = 12
private const val CODE_HEADER_SIZE = 16
private const val CODE_INSNS_SIZE_OFFSET = 12

private const val TYPE_STRING_IDS = 0x0001
private const val TYPE_TYPE_IDS = 0x0002
private const val TYPE_PROTO_IDS = 0x0003
private const val TYPE_FIELD_IDS = 0x0004
private const val TYPE_METHOD_IDS = 0x0005
private const val TYPE_CLASS_DEFS = 0x0006
private const val TYPE_FIRST_DATA_SECTION = 0x1000
private const val TYPE_STRING_DATA = 0x2002

private val DEFAULT_DEX_VERSION = byteArrayOf(0x30, 0x33, 0x35, 0x00)

// Single header_item at offset zero
private val MAP_LIST_ANCHOR = byteArrayOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0)

private val SECTION_HEADER_FIELDS = mapOf(
    TYPE_STRING_IDS to STRING_IDS_OFFSET,
    TYPE_TYPE_IDS to TYPE_IDS_OFFSET,
    TYPE_PROTO_IDS to PROTO_IDS_OFFSET,
    TYPE_FIELD_IDS to FIELD_IDS_OFFSET,
    TYPE_METHOD_IDS to METHOD_IDS_OFFSET,
    TYPE_CLASS_DEFS to CLASS_DEFS_OFFSET,
)

// Original dex indices required for native method restoration
internal class PayloadDex private constructor(
    private val payload: ByteArray,
    val start: Int,
    val size: Int,
) {
    private fun int(offset: Int) = payload.readInt(start + within(offset, 4))

    private fun short(offset: Int) = payload.readShort(start + within(offset, 2))

    private fun within(offset: Int, width: Int): Int {
        if (offset < 0 || offset > size - width) {
            throw PatchException("Payload dex at $start reads $width bytes out of bounds at $offset (size $size)")
        }
        return offset
    }

    private val stringIdsSize = int(STRING_IDS_OFFSET)
    private val stringIdsOffset = int(STRING_IDS_OFFSET + 4)
    private val typeIdsSize = int(TYPE_IDS_OFFSET)
    private val typeIdsOffset = int(TYPE_IDS_OFFSET + 4)
    private val protoIdsSize = int(PROTO_IDS_OFFSET)
    private val protoIdsOffset = int(PROTO_IDS_OFFSET + 4)
    private val fieldIdsSize = int(FIELD_IDS_OFFSET)
    private val fieldIdsOffset = int(FIELD_IDS_OFFSET + 4)
    private val methodIdsSize = int(METHOD_IDS_OFFSET)
    private val methodIdsOffset = int(METHOD_IDS_OFFSET + 4)
    private val classDefsSize = int(CLASS_DEFS_OFFSET)
    private val classDefsOffset = int(CLASS_DEFS_OFFSET + 4)

    init {
        requireTable(stringIdsOffset, stringIdsSize, 4, "string_ids")
        requireTable(typeIdsOffset, typeIdsSize, 4, "type_ids")
        requireTable(protoIdsOffset, protoIdsSize, PROTO_ID_SIZE, "proto_ids")
        requireTable(fieldIdsOffset, fieldIdsSize, FIELD_ID_SIZE, "field_ids")
        requireTable(methodIdsOffset, methodIdsSize, METHOD_ID_SIZE, "method_ids")
        requireTable(classDefsOffset, classDefsSize, CLASS_DEF_SIZE, "class_defs")
    }

    private fun requireTable(offset: Int, count: Int, entry: Int, name: String) {
        if (count < 0 || offset < 0 || offset.toLong() + count.toLong() * entry > size) {
            throw PatchException(
                "Payload dex at $start has an out-of-range $name table: $count entries at $offset (size $size)",
            )
        }
    }

    private fun requireMember(index: Int, count: Int, table: String) {
        if (index < 0 || index >= count) {
            throw PatchException("Payload dex at $start references $table $index of $count")
        }
    }

    private fun uleb(offset: Int): Pair<Int, Int> {
        var value = 0
        var cursor = offset
        for (shift in 0..28 step 7) {
            val byte = payload[start + bounded(cursor++)].toInt()
            if (shift == 28 && byte and 0x7F > 0x0F) {
                throw PatchException("Payload dex at $start has a LEB128 value past 32 bits at $offset")
            }
            value = value or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) return value to cursor
        }
        throw PatchException("Payload dex at $start has an unterminated LEB128 value at $offset")
    }

    private fun bounded(offset: Int): Int {
        if (offset < 0 || offset >= size) {
            throw PatchException("Offset $offset outside payload dex at $start (size $size)")
        }
        return offset
    }

    private fun stringAt(index: Int): String {
        requireMember(index, stringIdsSize, "string")
        val (units, data) = uleb(int(stringIdsOffset + 4 * index))
        var cursor = data
        val chars = StringBuilder()
        while (true) {
            val lead = payload[start + bounded(cursor)].toInt() and 0xFF
            when {
                lead == 0 -> {
                    if (chars.length != units) invalidString(cursor, "declares $units units but decodes ${chars.length}")
                    return chars.toString()
                }
                lead < 0x80 -> {
                    chars.append(lead.toChar())
                    cursor += 1
                }
                lead and 0xE0 == 0xC0 -> {
                    val value = ((lead and 0x1F) shl 6) or continuation(cursor + 1)
                    if (value in 0x01..0x7F) invalidString(cursor, "overlong two-byte sequence")
                    chars.append(value.toChar())
                    cursor += 2
                }
                lead and 0xF0 == 0xE0 -> {
                    val value = ((lead and 0x0F) shl 12) or (continuation(cursor + 1) shl 6) or continuation(cursor + 2)
                    if (value < 0x800) invalidString(cursor, "overlong three-byte sequence")
                    chars.append(value.toChar())
                    cursor += 3
                }
                else -> invalidString(cursor, "invalid lead byte")
            }
        }
    }

    private fun continuation(offset: Int): Int {
        val byte = payload[start + bounded(offset)].toInt() and 0xFF
        if (byte and 0xC0 != 0x80) invalidString(offset, "truncated multi-byte sequence")
        return byte and 0x3F
    }

    private fun invalidString(offset: Int, reason: String): Nothing =
        throw PatchException("Payload dex at $start has invalid MUTF-8 at $offset: $reason")

    private fun typeDescriptor(index: Int): String {
        requireMember(index, typeIdsSize, "type")
        return stringAt(int(typeIdsOffset + 4 * index))
    }

    private fun nameOf(method: Int): String {
        requireMember(method, methodIdsSize, "method")
        return stringAt(int(methodIdsOffset + METHOD_ID_SIZE * method + 4))
    }

    private fun returnTypeOf(method: Int): String {
        requireMember(method, methodIdsSize, "method")
        val proto = short(methodIdsOffset + METHOD_ID_SIZE * method + 2)
        requireMember(proto, protoIdsSize, "proto")
        return typeDescriptor(int(protoIdsOffset + PROTO_ID_SIZE * proto + 4))
    }

    private val classDataByDescriptor by lazy {
        (0 until classDefsSize)
            .map { classDefsOffset + CLASS_DEF_SIZE * it }
            .groupBy({ typeDescriptor(int(it)) }, { int(it + CLASS_DATA_FIELD) })
    }

    fun fieldIndexOf(classDescriptor: String, name: String): Int? {
        val type = typeIndexOf(classDescriptor) ?: return null

        return (0 until fieldIdsSize).firstOrNull {
            val item = fieldIdsOffset + FIELD_ID_SIZE * it
            short(item) == type && stringAt(int(item + 4)) == name
        }
    }

    fun methodIndexOf(classDescriptor: String, name: String, shorty: String): Int? {
        val type = typeIndexOf(classDescriptor) ?: return null

        return (0 until methodIdsSize).firstOrNull {
            val item = methodIdsOffset + METHOD_ID_SIZE * it
            short(item) == type && stringAt(int(item + 4)) == name && shortyOf(short(item + 2)) == shorty
        }
    }

    private fun typeIndexOf(classDescriptor: String) =
        (0 until typeIdsSize).firstOrNull { typeDescriptor(it) == classDescriptor }

    private fun shortyOf(proto: Int): String {
        requireMember(proto, protoIdsSize, "proto")
        return stringAt(int(protoIdsOffset + PROTO_ID_SIZE * proto))
    }

    fun reseal() = payload.sealDexAt(start, size)

    fun bodiesOf(classDescriptor: String, name: String): List<MethodBody> {
        val bodies = mutableListOf<MethodBody>()

        for (classData in classDataByDescriptor[classDescriptor].orEmpty()) {
            if (classData == 0) continue

            var cursor = classData
            val counts = IntArray(4)
            for (section in counts.indices) {
                val (count, next) = uleb(cursor)
                counts[section] = count
                cursor = next
            }
            if (counts.any { it < 0 } ||
                counts[0].toLong() + counts[1] > fieldIdsSize ||
                counts[2].toLong() + counts[3] > methodIdsSize
            ) {
                throw PatchException(
                    "Payload dex at $start has an out-of-range class_data_item: " +
                        "${counts[0]}+${counts[1]} fields, ${counts[2]}+${counts[3]} methods",
                )
            }
            repeat(counts[0] + counts[1]) {
                cursor = uleb(uleb(cursor).second).second
            }

            for (kind in 2..3) {
                var method = 0L
                repeat(counts[kind]) {
                    val (diff, afterDiff) = uleb(cursor)
                    method += diff.toLong() and 0xFFFFFFFFL
                    if (method >= methodIdsSize) {
                        throw PatchException(
                            "Payload dex at $start has a method index $method past its $methodIdsSize entries",
                        )
                    }
                    val (_, afterFlags) = uleb(afterDiff)
                    val (codeOffset, afterCode) = uleb(afterFlags)
                    cursor = afterCode
                    val methodIndex = method.toInt()

                    if (codeOffset != 0 && nameOf(methodIndex) == name) {
                        within(codeOffset, CODE_HEADER_SIZE)
                        val insns = int(codeOffset + CODE_INSNS_SIZE_OFFSET)
                        if (insns < 0 || codeOffset.toLong() + CODE_HEADER_SIZE + insns.toLong() * 2 > size) {
                            throw PatchException(
                                "Payload dex at $start has a code item running past its end at $codeOffset",
                            )
                        }
                        bodies += MethodBody(
                            dex = this,
                            payload = payload,
                            codeOffset = start + codeOffset,
                            returnType = returnTypeOf(methodIndex),
                            description = "$classDescriptor->$name",
                        )
                    }
                }
            }
        }

        return bodies
    }

    companion object {
        fun split(payload: ByteArray): List<PayloadDex> {
            val starts = mutableListOf<Int>()
            var cursor = 0
            while (true) {
                val found = payload.indexOfDexHeader(cursor)
                if (found < 0) break
                starts += found
                cursor = found + 1
            }

            if (starts.isEmpty()) {
                throw PatchException(
                    "No dex header found after Ijiami substitution. Unsupported or invalid payload.",
                )
            }

            // First dex header inside the opaque block
            if (starts.first() != 0) starts.add(0, 0)

            val version = starts.getOrNull(1)
                ?.let { payload.copyOfRange(it + DEX_MAGIC.size, it + 8) }
                ?: DEFAULT_DEX_VERSION
            val firstSize = recoverHeadBlock(payload, starts.getOrElse(1) { payload.size }, version)

            val sizes = starts.mapIndexed { index, dexStart ->
                if (index == 0) firstSize else payload.readInt(dexStart + FILE_SIZE_OFFSET)
            }

            starts.forEachIndexed { index, dexStart ->
                val end = dexStart.toLong() + sizes[index]
                val next = starts.getOrNull(index + 1)

                val fits = if (next != null) end == next.toLong() else end <= payload.size
                if (sizes[index] <= 0 || !fits) {
                    throw PatchException(
                        "Invalid size for payload dex $index: ${sizes[index]} bytes, " +
                            "available: ${(next ?: payload.size) - dexStart}",
                    )
                }
            }

            return starts.mapIndexed { index, dexStart -> PayloadDex(payload, dexStart, sizes[index]) }
        }

        fun recoverHeadBlock(payload: ByteArray, limit: Int, version: ByteArray): Int {
            val anchor = payload.lastIndexOfMapList(limit)
                ?: throw PatchException("Map list not found in first payload dex")

            val mapOffset = anchor - 4
            if (mapOffset < 0 || mapOffset + 4 > payload.size) {
                throw PatchException("Map list offset out of bounds in first payload dex")
            }

            val entries = payload.readInt(mapOffset)
            val room = (limit.toLong() - mapOffset - 4) / MAP_ENTRY_SIZE
            if (entries < 0 || entries > room) {
                throw PatchException("Map list entry count out of range in first payload dex: $entries")
            }

            val dexSize = mapOffset + 4 + entries * MAP_ENTRY_SIZE
            if (dexSize > limit) {
                throw PatchException("Map list exceeds first payload dex boundary: $dexSize > $limit")
            }

            val sections = (0 until entries).associate { entry ->
                val base = mapOffset + 4 + entry * MAP_ENTRY_SIZE
                payload.readShort(base) to (payload.readInt(base + 4) to payload.readInt(base + 8))
            }

            val (stringCount, stringDataOffset) = sections[TYPE_STRING_DATA]
                ?: throw PatchException("Missing string_data section in first payload dex")
            val (declaredStrings, stringIdsOffset) = sections[TYPE_STRING_IDS]
                ?: throw PatchException("Missing string_ids section in first payload dex")
            if (declaredStrings != stringCount) {
                throw PatchException("String count mismatch in first payload dex: $declaredStrings != $stringCount")
            }
            if (stringCount < 0 || stringIdsOffset < DEX_HEADER_SIZE || stringDataOffset < 0 ||
                stringIdsOffset.toLong() + 4L * stringCount > dexSize
            ) {
                throw PatchException(
                    "String section out of range in first payload dex: " +
                        "ids@$stringIdsOffset data@$stringDataOffset count $stringCount",
                )
            }

            val recovered = ByteArray(stringIdsOffset + 4 * stringCount)
            DEX_MAGIC.copyInto(recovered)
            version.copyInto(recovered, DEX_MAGIC.size)
            recovered.writeInt(FILE_SIZE_OFFSET, dexSize)
            recovered.writeInt(HEADER_SIZE_OFFSET, DEX_HEADER_SIZE)
            recovered.writeInt(ENDIAN_TAG_OFFSET, ENDIAN_TAG)
            recovered.writeInt(MAP_OFF_OFFSET, mapOffset)
            SECTION_HEADER_FIELDS.forEach { (type, field) ->
                val (count, offset) = sections[type] ?: (0 to 0)
                recovered.writeInt(field, count)
                recovered.writeInt(field + 4, offset)
            }
            val dataOffset = sections.filterKeys { it >= TYPE_FIRST_DATA_SECTION }
                .values.minOf { (_, offset) -> offset }
            recovered.writeInt(DATA_OFFSET, dexSize - dataOffset)
            recovered.writeInt(DATA_OFFSET + 4, dataOffset)

            var cursor = stringDataOffset
            for (index in 0 until stringCount) {
                recovered.writeInt(stringIdsOffset + 4 * index, cursor)
                var scan = payload.uleb128End(cursor, dexSize)
                while (scan < dexSize && payload[scan] != 0.toByte()) scan++
                if (scan >= dexSize) throw PatchException("Unterminated string at $cursor in first payload dex")
                cursor = scan + 1
            }

            val lastDisagreement = recovered.indices.lastOrNull { payload[it] != recovered[it] }
            if (lastDisagreement != null && lastDisagreement >= OPAQUE_BLOCK_SIZE) {
                throw PatchException(
                    "Header recovery mismatch at byte $lastDisagreement, past the block boundary $OPAQUE_BLOCK_SIZE",
                )
            }

            recovered.copyInto(payload, 0, 0, minOf(OPAQUE_BLOCK_SIZE, recovered.size))
            payload.sealDex(dexSize)

            return dexSize
        }

        private fun ByteArray.isDexHeaderAt(index: Int): Boolean {
            if (index + DEX_HEADER_SIZE > size) return false
            if (DEX_MAGIC.indices.any { this[index + it] != DEX_MAGIC[it] }) return false
            val version = index + DEX_MAGIC.size
            if ((version until version + 3).any { this[it] < '0'.code.toByte() || this[it] > '9'.code.toByte() }) {
                return false
            }
            return this[version + 3] == 0.toByte() &&
                readInt(index + HEADER_SIZE_OFFSET) == DEX_HEADER_SIZE &&
                readInt(index + ENDIAN_TAG_OFFSET) == ENDIAN_TAG
        }

        private fun ByteArray.indexOfDexHeader(from: Int): Int {
            var index = from
            while (index + DEX_HEADER_SIZE <= size) {
                if (isDexHeaderAt(index)) return index
                index++
            }
            return -1
        }

        private fun ByteArray.uleb128End(offset: Int, limit: Int): Int {
            var cursor = offset
            while (cursor < limit && this[cursor].toInt() and 0x80 != 0) cursor++
            if (cursor >= limit) throw PatchException("Unterminated string length at $offset in first payload dex")
            return cursor + 1
        }

        private fun ByteArray.lastIndexOfMapList(limit: Int): Int? {
            for (index in limit - MAP_LIST_ANCHOR.size downTo 0) {
                if (MAP_LIST_ANCHOR.indices.all { this[index + it] == MAP_LIST_ANCHOR[it] }) return index
            }
            return null
        }
    }
}
