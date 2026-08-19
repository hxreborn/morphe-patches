/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.shared

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.io.RandomAccessFile

internal const val RUST_CORE = "libmail_uniffi.so"
internal const val ARM64 = "arm64-v8a"
internal const val ARM32 = "armeabi-v7a"
internal const val X86_64 = "x86_64"

internal fun File.replaceTrailingMasked(pattern: ByteArray, mask: ByteArray, replacement: ByteArray): Boolean {
    require(pattern.size == mask.size) { "Mask must be the same length as the pattern" }
    require(replacement.size <= pattern.size) { "Replacement cannot be longer than the pattern" }

    RandomAccessFile(this, "rw").use { core ->
        val bytes = ByteArray(core.length().toInt())
        core.readFully(bytes)

        val index = bytes.indexOfMasked(pattern, mask)
        if (index < 0) return false

        if (bytes.indexOfMasked(pattern, mask, index + 1) >= 0) {
            throw PatchException("Pattern matches $name more than once")
        }

        core.seek((index + pattern.size - replacement.size).toLong())
        core.write(replacement)
    }

    return true
}

private fun ByteArray.indexOfMasked(pattern: ByteArray, mask: ByteArray, startIndex: Int = 0): Int {
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
