/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.jiagu

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.jiagu.zstd.Zstd

private const val DEX_HEADER_SIZE = 0x70
private const val DEX_MAGIC_FIRST = 'd'.code

private const val FRAME_HEADER_DESCRIPTOR = 0xA0
private const val BLOCK_MAX = 131_072
private const val BLOCK_RAW = 0
private const val BLOCK_LAST = 1

private val ZSTD_MAGIC = byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())

internal class JiaguPayloadDex(frame: ByteArray) {
    private val headerXor: Int
    val dex: ByteArray

    init {
        val raw = Zstd.decompress(frame, 0, frame.size)
        if (raw.size < DEX_HEADER_SIZE) {
            throw PatchException("Decompressed Jiagu part too short: ${raw.size} bytes, minimum $DEX_HEADER_SIZE")
        }

        headerXor = (raw[0].toInt() and 0xFF) xor DEX_MAGIC_FIRST
        dex = raw.copyOf().apply { obfuscateHeader(headerXor) }
    }

    fun reframe(edited: ByteArray): ByteArray {
        val body = edited.copyOf().apply { obfuscateHeader(headerXor) }

        val frame = ByteArrayBuilder(ZSTD_MAGIC.size + 5 + body.size + blockHeaders(body.size))
        frame.write(ZSTD_MAGIC)
        frame.write(FRAME_HEADER_DESCRIPTOR)
        frame.writeInt(body.size)

        var offset = 0
        while (offset < body.size) {
            val length = minOf(BLOCK_MAX, body.size - offset)
            val last = if (offset + length >= body.size) BLOCK_LAST else 0
            frame.writeHeader(last or (BLOCK_RAW shl 1) or (length shl 3))
            frame.write(body, offset, length)
            offset += length
        }

        return frame.bytes()
    }

    private companion object {
        fun blockHeaders(size: Int) = 3 * ((size + BLOCK_MAX - 1) / BLOCK_MAX).coerceAtLeast(1)

        fun ByteArray.obfuscateHeader(key: Int) {
            for (at in 0 until DEX_HEADER_SIZE) this[at] = (this[at].toInt() xor key).toByte()
        }
    }
}

private class ByteArrayBuilder(capacity: Int) {
    private val buffer = ByteArray(capacity)
    private var at = 0

    fun bytes() = if (at == buffer.size) buffer else buffer.copyOf(at)

    fun write(value: Int) {
        buffer[at++] = value.toByte()
    }

    fun write(source: ByteArray, offset: Int = 0, length: Int = source.size) {
        source.copyInto(buffer, at, offset, offset + length)
        at += length
    }

    fun writeInt(value: Int) {
        repeat(4) { write(value ushr (it * 8)) }
    }

    fun writeHeader(value: Int) {
        repeat(3) { write(value ushr (it * 8)) }
    }
}
