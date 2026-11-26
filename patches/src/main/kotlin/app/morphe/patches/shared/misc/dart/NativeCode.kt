/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dart

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.readShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val ELF_PROGRAM_HEADERS = 32
private const val ELF_PROGRAM_HEADER_SIZE = 54
private const val ELF_PROGRAM_HEADER_COUNT = 56
private const val ELF_SEGMENT_FLAGS = 4
private const val ELF_SEGMENT_OFFSET = 8
private const val ELF_SEGMENT_FILE_SIZE = 32
private const val ELF_SEGMENT_MEMORY_SIZE = 40
private const val ELF_LOAD = 1
private const val ELF_EXECUTE = 1
private const val ELF_SECTIONS = 40
private const val ELF_SECTION_HEADER_SIZE = 58
private const val ELF_SECTION_HEADER_COUNT = 60
private const val ELF_SECTION_OFFSET = 24
private const val ELF_SECTION_SIZE = 32

internal fun ByteArray.appendNativeCode(code: ByteArray): Int {
    val elf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    val headers = elf.getLong(ELF_PROGRAM_HEADERS).toInt()
    val headerSize = readShort(ELF_PROGRAM_HEADER_SIZE)
    val segments = (0 until readShort(ELF_PROGRAM_HEADER_COUNT)).map { headers + it * headerSize }
        .filter { readInt(it) == ELF_LOAD }
    val executable = segments.single { readInt(it + ELF_SEGMENT_FLAGS) and ELF_EXECUTE != 0 }
    val segmentOffset = elf.getLong(executable + ELF_SEGMENT_OFFSET).toInt()
    val originalSize = elf.getLong(executable + ELF_SEGMENT_FILE_SIZE)
    if (elf.getLong(executable + ELF_SEGMENT_MEMORY_SIZE) != originalSize) {
        throw PatchException("Unexpected Dart executable segment memory size")
    }
    val segmentEnd = segmentOffset + originalSize.toInt()
    val nextSegment = segments.map { elf.getLong(it + ELF_SEGMENT_OFFSET).toInt() }
        .filter { it >= segmentEnd }.minOrNull()
        ?: throw PatchException("Could not find Dart executable segment boundary")
    val start = (segmentEnd + INSTRUCTION_BYTES - 1) / INSTRUCTION_BYTES * INSTRUCTION_BYTES
    val end = start + code.size
    if (end > nextSegment || (segmentEnd until end).any { this[it] != 0.toByte() }) {
        throw PatchException("Insufficient executable segment padding")
    }
    code.copyInto(this, start)
    elf.putLong(executable + ELF_SEGMENT_FILE_SIZE, (end - segmentOffset).toLong())
    elf.putLong(executable + ELF_SEGMENT_MEMORY_SIZE, (end - segmentOffset).toLong())
    val sections = elf.getLong(ELF_SECTIONS).toInt()
    val sectionSize = readShort(ELF_SECTION_HEADER_SIZE)
    (0 until readShort(ELF_SECTION_HEADER_COUNT)).map { sections + it * sectionSize }.filter {
        elf.getLong(it + ELF_SECTION_OFFSET) + elf.getLong(it + ELF_SECTION_SIZE) == segmentEnd.toLong()
    }.forEach {
        elf.putLong(it + ELF_SECTION_SIZE, end - elf.getLong(it + ELF_SECTION_OFFSET))
    }
    return start
}
