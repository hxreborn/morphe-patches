/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ELF_MACHINE_OFFSET = 18
private const val ELF_ARM64 = 183
private const val ELF_PROGRAM_TABLE_OFFSET = 32
private const val ELF_PROGRAM_ENTRY_SIZE_OFFSET = 54
private const val ELF_PROGRAM_COUNT_OFFSET = 56
private const val SEGMENT_LOAD = 1
private const val SEGMENT_EXECUTABLE = 1
private const val SEGMENT_WRITABLE = 2
private const val MODE_CACHE_BYTES = 8
private const val SEGMENT_FLAGS_OFFSET = 4
private const val SEGMENT_FILE_OFFSET = 8
private const val SEGMENT_ADDRESS_OFFSET = 16
private const val SEGMENT_FILE_SIZE_OFFSET = 32
private const val SEGMENT_MEMORY_SIZE_OFFSET = 40
private const val SEGMENT_ALIGNMENT_OFFSET = 48
private const val MIN_MAPPING_ALIGNMENT = 4096L
private const val ELF_CLASS_OFFSET = 4
private const val ELF_CLASS_64 = 2
private const val BYTE_MASK = 0xff
private const val HALFWORD_MASK = 0xffff

internal class ThemeElfImage(val bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    private data class Segment(val header: Int, val offset: Int, val address: Int, val size: Int, val flags: Int)

    private val segments: List<Segment>
    private val mappingAlignment: Long
    val codeStart: Int
    val codeEnd: Int
    val hookOffset: Int
    val modeCacheAddress: Int
    val hookAddress: Int

    init {
        if (bytes[ELF_CLASS_OFFSET].toInt() and BYTE_MASK != ELF_CLASS_64 ||
            buffer.getShort(ELF_MACHINE_OFFSET).toInt() and HALFWORD_MASK != ELF_ARM64
        ) {
            throw PatchException("Expected an arm64 Dart library")
        }
        val table = buffer.getLong(ELF_PROGRAM_TABLE_OFFSET).toInt()
        val entrySize = buffer.getShort(ELF_PROGRAM_ENTRY_SIZE_OFFSET).toInt() and HALFWORD_MASK
        val count = buffer.getShort(ELF_PROGRAM_COUNT_OFFSET).toInt() and HALFWORD_MASK
        segments = (0 until count).mapNotNull { index ->
            val header = table + index * entrySize
            if (buffer.getInt(header) != SEGMENT_LOAD) return@mapNotNull null
            Segment(
                header,
                buffer.getLong(header + SEGMENT_FILE_OFFSET).toInt(),
                buffer.getLong(header + SEGMENT_ADDRESS_OFFSET).toInt(),
                buffer.getLong(header + SEGMENT_FILE_SIZE_OFFSET).toInt(),
                buffer.getInt(header + SEGMENT_FLAGS_OFFSET),
            )
        }
        mappingAlignment = segments.maxOf { buffer.getLong(it.header + SEGMENT_ALIGNMENT_OFFSET).coerceAtLeast(MIN_MAPPING_ALIGNMENT) }
        val code = segments.single { it.flags and SEGMENT_EXECUTABLE != 0 }
        val writable = segments.single { it.flags and SEGMENT_WRITABLE != 0 }
        modeCacheAddress = writable.address + buffer.getLong(writable.header + SEGMENT_MEMORY_SIZE_OFFSET).toInt()
        codeStart = code.offset
        codeEnd = code.offset + code.size
        val memorySize = buffer.getLong(code.header + SEGMENT_MEMORY_SIZE_OFFSET)
        if (memorySize < code.size || memorySize > Int.MAX_VALUE) {
            throw PatchException("The Dart library has an unsupported executable memory size")
        }
        hookOffset = code.offset + memorySize.toInt()
        hookAddress = code.address + memorySize.toInt()
    }

    fun instructionAddress(offset: Int): Int {
        val code = segments.single { it.flags and SEGMENT_EXECUTABLE != 0 }
        return offset - code.offset + code.address
    }

    fun install(hooks: ByteArray, cacheBytes: Int = MODE_CACHE_BYTES) {
        val code = segments.single { it.flags and SEGMENT_EXECUTABLE != 0 }
        val following = segments.filter { it.offset > code.offset }.minOf { it.offset }
        if (hookOffset + hooks.size > following || (codeEnd until hookOffset + hooks.size).any { bytes[it] != 0.toByte() }) {
            throw PatchException("The Dart library has insufficient unused executable padding for theme hooks")
        }
        val size = (hookOffset - code.offset).toLong() + hooks.size
        val memorySize = buffer.getLong(code.header + SEGMENT_MEMORY_SIZE_OFFSET)
        val virtualEnd = code.address.toLong() + maxOf(size, memorySize)
        if (segments.any { it.header != code.header && it.address >= code.address && it.address.toLong() < virtualEnd }) {
            throw PatchException("The Dart library has insufficient unused virtual space for theme hooks")
        }
        val writable = segments.single { it.flags and SEGMENT_WRITABLE != 0 }
        val writableSize = buffer.getLong(writable.header + SEGMENT_MEMORY_SIZE_OFFSET)
        val cacheEnd = writable.address.toLong() + writableSize + cacheBytes
        if (segments.any { it.header != writable.header && it.address >= writable.address && it.address.toLong() < cacheEnd }) {
            throw PatchException("The Dart library has insufficient unused virtual space for theme cache")
        }
        requireUnusedPages(code, hookAddress.toLong(), virtualEnd, "theme hooks")
        requireUnusedPages(writable, modeCacheAddress.toLong(), cacheEnd, "theme cache")
        hooks.copyInto(bytes, hookOffset)
        buffer.putLong(code.header + SEGMENT_FILE_SIZE_OFFSET, size)
        buffer.putLong(code.header + SEGMENT_MEMORY_SIZE_OFFSET, size)
        buffer.putLong(writable.header + SEGMENT_MEMORY_SIZE_OFFSET, writableSize + cacheBytes)
    }

    private fun requireUnusedPages(segment: Segment, start: Long, end: Long, name: String) {
        if (start >= end) return
        for (other in segments) {
            if (other.header == segment.header) continue
            val otherStart = other.address.toLong() / mappingAlignment * mappingAlignment
            val otherEnd = other.address.toLong() + buffer.getLong(other.header + SEGMENT_MEMORY_SIZE_OFFSET)
            val mappedEnd = (otherEnd + mappingAlignment - 1) / mappingAlignment * mappingAlignment
            if (start < mappedEnd && end > otherStart) {
                throw PatchException("The Dart library has overlapping load pages for $name")
            }
        }
    }
}
