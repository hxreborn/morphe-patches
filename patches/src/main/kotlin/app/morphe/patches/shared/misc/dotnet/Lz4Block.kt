/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException

private const val LENGTH_MASK = 0x0f
private const val LITERAL_LENGTH_SHIFT = 4
private const val EXTENDED_LENGTH = 15
private const val LENGTH_BYTE_CONTINUES = 255
private const val MIN_MATCH_LENGTH = 4

internal fun decompressLz4Block(source: ByteArray, start: Int, end: Int, decompressedSize: Int): ByteArray {
    val output = ByteArray(decompressedSize)
    var input = start
    var written = 0

    fun readLength(initial: Int): Int {
        var length = initial
        if (initial == EXTENDED_LENGTH) {
            do {
                val next = source[input++].toInt() and 0xff
                length += next
            } while (next == LENGTH_BYTE_CONTINUES)
        }
        return length
    }

    while (input < end) {
        val token = source[input++].toInt() and 0xff
        val literalLength = readLength(token ushr LITERAL_LENGTH_SHIFT)
        System.arraycopy(source, input, output, written, literalLength)
        input += literalLength
        written += literalLength
        if (input >= end) break

        val matchOffset = (source[input].toInt() and 0xff) or ((source[input + 1].toInt() and 0xff) shl 8)
        input += Short.SIZE_BYTES
        if (matchOffset == 0 || matchOffset > written) throw PatchException("Invalid LZ4 match offset $matchOffset")

        val matchLength = readLength(token and LENGTH_MASK) + MIN_MATCH_LENGTH
        repeat(matchLength) {
            output[written] = output[written - matchOffset]
            written++
        }
    }

    if (written != decompressedSize) throw PatchException("LZ4 block decompressed to $written bytes, expected $decompressedSize")
    return output
}
