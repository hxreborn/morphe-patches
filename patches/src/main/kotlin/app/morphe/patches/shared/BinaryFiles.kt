/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.io.RandomAccessFile

internal val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a)
internal const val DEX_CHECKSUM_OFFSET = 8
internal const val DEX_SIGNATURE_OFFSET = 12
internal const val DEX_FILE_SIZE_OFFSET = 32
internal const val DEX_MAP_OFF_OFFSET = 52
internal const val DEX_HEADER_SIZE = 0x70
internal const val DEX_MAP_ITEM_SIZE = 12

internal fun ByteArray.readInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

internal fun ByteArray.writeInt(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
}

internal fun ByteArray.writeShort(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
}

internal fun ByteArray.readShort(offset: Int) =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

internal fun File.replaceAsciiInPlace(old: String, new: String): Boolean {
    require(old.length == new.length) { "Replacement must keep the byte length" }

    val original = readBytes().toString(Charsets.ISO_8859_1)
    val patched = original.replace(old, new)

    if (patched == original) return false

    writeBytes(patched.toByteArray(Charsets.ISO_8859_1))
    return true
}

internal fun File.replaceTrailingMasked(pattern: ByteArray, mask: ByteArray, replacement: ByteArray) =
    replaceMasked(pattern, mask, mapOf(pattern.size - replacement.size to replacement))

internal fun File.replaceMasked(
    pattern: ByteArray,
    mask: ByteArray,
    replacementsByOffset: Map<Int, ByteArray>,
): Boolean {
    require(pattern.size == mask.size) { "Mask must be the same length as the pattern" }
    require(replacementsByOffset.all { (offset, bytes) -> offset >= 0 && offset + bytes.size <= pattern.size }) {
        "Replacements must fall inside the pattern"
    }

    RandomAccessFile(this, "rw").use { file ->
        val bytes = ByteArray(file.length().toInt())
        file.readFully(bytes)

        val index = bytes.indexOfMasked(pattern, mask)
        if (index < 0) return false

        if (bytes.indexOfMasked(pattern, mask, index + 1) >= 0) {
            throw PatchException("Pattern matches $name more than once")
        }

        for ((offset, replacement) in replacementsByOffset) {
            file.seek((index + offset).toLong())
            file.write(replacement)
        }
    }

    return true
}

internal fun File.replaceMaskedEvery(pattern: ByteArray, mask: ByteArray, offset: Int, replacement: ByteArray): Int {
    require(pattern.size == mask.size) { "Mask must be the same length as the pattern" }
    require(offset >= 0 && offset + replacement.size <= pattern.size) { "Replacement must fall inside the pattern" }

    val bytes = readBytes()
    var count = 0
    var index = bytes.indexOfMasked(pattern, mask)
    while (index >= 0) {
        replacement.copyInto(bytes, index + offset)
        count++
        index = bytes.indexOfMasked(pattern, mask, index + pattern.size)
    }
    if (count > 0) writeBytes(bytes)
    return count
}

internal fun ByteArray.indexOfMasked(pattern: ByteArray, mask: ByteArray, startIndex: Int = 0): Int {
    candidate@ for (index in startIndex..size - pattern.size) {
        for (offset in pattern.indices) {
            val masked = mask[offset].toInt()
            val actual = this[index + offset].toInt() and masked
            val expected = pattern[offset].toInt() and masked

            if (actual != expected) continue@candidate
        }
        return index
    }
    return -1
}
