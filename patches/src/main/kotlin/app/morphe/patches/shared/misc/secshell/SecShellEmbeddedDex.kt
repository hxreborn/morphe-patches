/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.secshell

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.DEX_CHECKSUM_OFFSET
import app.morphe.patches.shared.DEX_FILE_SIZE_OFFSET
import app.morphe.patches.shared.DEX_HEADER_SIZE
import app.morphe.patches.shared.DEX_MAP_ITEM_SIZE
import app.morphe.patches.shared.DEX_SIGNATURE_OFFSET
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.CRC32

private const val CIPHER_HEAD_SIZE = 0x20000
private const val ENDIAN_TAG = 0x12345678
private const val SIGNED_OFFSET = 32
private const val ADLER_MODULUS = 65521
private const val FORGE_PAIRS = 6

private const val TYPE_HEADER = 0x0000
private const val TYPE_STRING_ID = 0x0001
private const val TYPE_METHOD_HANDLE = 0x0008
private const val TYPE_MAP_LIST = 0x1000
private const val TYPE_STRING_DATA = 0x2002
private const val MIN_MAP_ITEMS = 5
private const val MAX_MAP_ITEMS = 40

private val ID_SECTIONS = 0x0001..0x0006

private fun ByteArray.xorHead(keystream: ByteArray) = copyOf().also {
    for (index in 0 until minOf(CIPHER_HEAD_SIZE, it.size)) {
        it[index] = (it[index].toInt() xor keystream[index].toInt()).toByte()
    }
}

private val FOOTER_MAGIC = "fdex".toByteArray()

private val MAP_LIST_START = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
    .putShort(TYPE_HEADER.toShort()).putShort(0).putInt(1).putInt(0)
    .putShort(TYPE_STRING_ID.toShort()).putShort(0).array()

internal class SecShellEmbeddedDex(bytes: ByteArray) : SecShellContainer {
    private class MapItem(val type: Int, val size: Int, val offset: Int)

    private class Slot(val start: Int, val size: Int, val mapItems: List<MapItem>)

    private data class FlippableBit(val lowOffset: Int, val bit: Int, val crcEffect: Int)

    private val blob = bytes.copyOf()

    private val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)

    private val slots: List<Slot> = run {
        val stubEnd = buffer.getInt(blob.size - FOOTER_MAGIC.size - 4)
        val found = mutableListOf<Slot>()
        var cursor = stubEnd
        while (true) {
            val hit = indexOfPattern(MAP_LIST_START, cursor + 4)
            if (hit < 0) break
            val mapList = hit - 4
            val items = mapItems(mapList)
            val mapOffset = items?.firstOrNull { it.type == TYPE_MAP_LIST }?.offset
            if (items == null || mapOffset == null || mapOffset > mapList) {
                cursor = hit
                continue
            }
            val end = mapList + 4 + items.size * DEX_MAP_ITEM_SIZE
            found += Slot(mapList - mapOffset, end - (mapList - mapOffset), items)
            cursor = end
        }
        if (found.isEmpty()) throw PatchException("SecShell classes.dex holds no payload dex")
        found
    }

    private val keystream = deriveKeystream()

    private fun mapItems(mapList: Int): List<MapItem>? {
        val count = buffer.getInt(mapList)
        if (count !in MIN_MAP_ITEMS..MAX_MAP_ITEMS || mapList + 4 + count * DEX_MAP_ITEM_SIZE > blob.size) return null
        val items = (0 until count).map {
            val item = mapList + 4 + it * DEX_MAP_ITEM_SIZE
            MapItem(buffer.getShort(item).toInt() and 0xFFFF, buffer.getInt(item + 4), buffer.getInt(item + 8))
        }
        val ordered = items.zipWithNext().all { (first, second) -> first.offset <= second.offset }
        return items.takeIf { ordered && it[1].offset == DEX_HEADER_SIZE }
    }

    private fun slotEnd(index: Int) = slots.getOrNull(index + 1)?.start ?: (slots[index].start + slots[index].size)

    override fun dexEntries() = slots.indices.map(::entryName)

    override fun read(name: String): ByteArray {
        val slot = slots[slotIndex(name)]
        return blob.copyOfRange(slot.start, slot.start + slot.size).xorHead(keystream)
    }

    override fun withEntries(replacements: Map<String, ByteArray>): ByteArray {
        val original = CRC32().apply { update(blob) }.value.toInt()
        val output = blob.copyOf()
        var freeStart = -1
        var freeEnd = -1
        replacements.forEach { (name, dex) ->
            val index = slotIndex(name)
            val start = slots[index].start
            val end = slotEnd(index)
            if (dex.size > end - start) {
                throw PatchException("Rebuilt $name is ${dex.size} bytes, its slot holds ${end - start}")
            }
            dex.xorHead(keystream).copyInto(output, start)
            if (end - (start + dex.size) > freeEnd - freeStart) {
                freeStart = start + dex.size
                freeEnd = end
            }
        }
        if (freeEnd - freeStart < ADLER_MODULUS + FORGE_PAIRS) {
            throw PatchException("No rebuilt payload dex left room to restore the classes.dex checksums")
        }
        seal(output, freeStart, original)
        return output
    }

    private fun entryName(index: Int) = "payload$index.dex"

    private fun slotIndex(name: String) =
        dexEntries().indexOf(name).takeIf { it >= 0 } ?: throw PatchException("SecShell payload has no $name")

    private fun deriveKeystream(): ByteArray {
        val version = blob.copyOfRange(0, DEX_CHECKSUM_OFFSET)
        val slot = slots.maxBy { it.mapItems.first { item -> item.type == TYPE_STRING_ID }.size }
        val region = blob.copyOfRange(slot.start, slot.start + slot.size)
        val plain = plainHead(region, slot, version)
        if (plain.size < CIPHER_HEAD_SIZE) throw PatchException("SecShell payload heads are too short to read")

        val keystream = ByteArray(CIPHER_HEAD_SIZE) { (region[it].toInt() xor plain[it].toInt()).toByte() }
        val dex = region.xorHead(keystream)
        restoreHeaderDigests(dex)
        for (index in DEX_CHECKSUM_OFFSET until SIGNED_OFFSET) {
            keystream[index] = (region[index].toInt() xor dex[index].toInt()).toByte()
        }

        slots.forEach { other ->
            val decoded = blob.copyOfRange(other.start, other.start + other.size).xorHead(keystream)
            val stored = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).getInt(DEX_CHECKSUM_OFFSET)
            if (stored != adlerOf(decoded, DEX_SIGNATURE_OFFSET)) {
                throw PatchException("SecShell payload dex at ${other.start} does not decipher with the derived key")
            }
        }
        return keystream
    }

    private fun plainHead(region: ByteArray, slot: Slot, version: ByteArray): ByteArray {
        val sections = slot.mapItems.associateBy { it.type }
        val dataOffset = slot.mapItems.filter { it.type > TYPE_METHOD_HANDLE || it.type < TYPE_HEADER }
            .minOf { it.offset }
        val header = ByteBuffer.allocate(DEX_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(version)
            (this as Buffer).position(DEX_FILE_SIZE_OFFSET)
            putInt(slot.size)
            putInt(DEX_HEADER_SIZE)
            putInt(ENDIAN_TAG)
            putInt(0)
            putInt(0)
            putInt(sections.getValue(TYPE_MAP_LIST).offset)
            ID_SECTIONS.forEach { type ->
                putInt(sections[type]?.size ?: 0)
                putInt(sections[type]?.offset ?: 0)
            }
            putInt(slot.size - dataOffset)
            putInt(dataOffset)
        }.array()

        val stringIds = sections.getValue(TYPE_STRING_ID)
        if (stringIds.offset != DEX_HEADER_SIZE) throw PatchException("SecShell payload string ids do not follow the header")
        val ids = ByteBuffer.allocate(stringIds.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        var cursor = sections.getValue(TYPE_STRING_DATA).offset
        repeat(stringIds.size) {
            ids.putInt(cursor)
            while (region[cursor].toInt() < 0) cursor++
            cursor++
            while (region[cursor].toInt() != 0) cursor++
            cursor++
        }
        return header + ids.array()
    }

    private fun seal(output: ByteArray, freeStart: Int, targetCrc: Int) {
        val lows = (freeStart until freeStart + FORGE_PAIRS).toList()
        lows.forEach {
            output[it] = 0
            output[it + ADLER_MODULUS] = -1
        }
        ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN).putInt(DEX_CHECKSUM_OFFSET, adlerOf(output, DEX_SIGNATURE_OFFSET))

        val current = CRC32().apply { update(output) }.value.toInt()
        val flippableBits = lows.flatMap { low ->
            val lowShift = Crc32Shift.zeroBytes(output.size - 1 - low)
            val highShift = Crc32Shift.zeroBytes(output.size - 1 - low - ADLER_MODULUS)
            (0 until Byte.SIZE_BITS).map { bit ->
                val register = Crc32Shift.byteRegister(1 shl bit)
                FlippableBit(low, bit, Crc32Shift.apply(lowShift, register) xor Crc32Shift.apply(highShift, register))
            }
        }
        val flips = Crc32Shift.solve(flippableBits.map { it.crcEffect }, current xor targetCrc)
            ?: throw PatchException("classes.dex checksum cannot be restored")
        flips.forEach { index ->
            val (low, bit) = flippableBits[index]
            output[low] = (output[low].toInt() xor (1 shl bit)).toByte()
            output[low + ADLER_MODULUS] = (output[low + ADLER_MODULUS].toInt() xor (1 shl bit)).toByte()
        }
        if (CRC32().apply { update(output) }.value.toInt() != targetCrc) {
            throw PatchException("classes.dex checksum restore failed")
        }
    }

    private fun indexOfPattern(pattern: ByteArray, from: Int): Int {
        var index = from
        val last = blob.size - pattern.size
        while (index <= last) {
            if (blob[index] == pattern[0] && (1 until pattern.size).all { blob[index + it] == pattern[it] }) return index
            index++
        }
        return -1
    }

    companion object {
        fun isEmbedded(bytes: ByteArray) =
            bytes.size > 8 && FOOTER_MAGIC.indices.all { bytes[bytes.size - FOOTER_MAGIC.size + it] == FOOTER_MAGIC[it] }

        private fun adlerOf(bytes: ByteArray, from: Int) =
            Adler32().apply { update(bytes, from, bytes.size - from) }.value.toInt()

        private fun restoreHeaderDigests(dex: ByteArray) {
            val signature = MessageDigest.getInstance("SHA-1").apply {
                update(dex, SIGNED_OFFSET, dex.size - SIGNED_OFFSET)
            }.digest()
            signature.copyInto(dex, DEX_SIGNATURE_OFFSET)
            ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN).putInt(DEX_CHECKSUM_OFFSET, adlerOf(dex, DEX_SIGNATURE_OFFSET))
        }
    }
}

private object Crc32Shift {
    private const val POLYNOMIAL = 0xEDB88320.toInt()

    private val table = IntArray(256) { index ->
        (0 until 8).fold(index) { value, _ -> if ((value and 1) != 0) (value ushr 1) xor POLYNOMIAL else value ushr 1 }
    }

    private val oneZeroByte = IntArray(32) { bit ->
        val register = 1 shl bit
        (register ushr 8) xor table[register and 0xFF]
    }

    fun byteRegister(value: Int) = table[value and 0xFF]

    fun apply(matrix: IntArray, vector: Int): Int {
        var result = 0
        for (bit in 0 until 32) if ((vector ushr bit) and 1 != 0) result = result xor matrix[bit]
        return result
    }

    fun zeroBytes(count: Int): IntArray {
        var result = IntArray(32) { 1 shl it }
        var power = oneZeroByte
        var remaining = count
        while (remaining > 0) {
            if (remaining and 1 != 0) result = multiply(power, result)
            power = multiply(power, power)
            remaining = remaining ushr 1
        }
        return result
    }

    fun solve(columns: List<Int>, target: Int): List<Int>? {
        val pivots = HashMap<Int, Pair<Int, Long>>()
        columns.forEachIndexed { index, column ->
            var vector = column
            var combination = 1L shl index
            for (bit in 31 downTo 0) {
                if ((vector ushr bit) and 1 == 0) continue
                val pivot = pivots[bit]
                if (pivot == null) {
                    pivots[bit] = vector to combination
                    break
                }
                vector = vector xor pivot.first
                combination = combination xor pivot.second
            }
        }
        var remaining = target
        var combination = 0L
        for (bit in 31 downTo 0) {
            if ((remaining ushr bit) and 1 == 0) continue
            val pivot = pivots[bit] ?: return null
            remaining = remaining xor pivot.first
            combination = combination xor pivot.second
        }
        return columns.indices.filter { (combination ushr it) and 1L != 0L }
    }

    private fun multiply(outer: IntArray, inner: IntArray) = IntArray(32) { apply(outer, inner[it]) }
}
