/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SECTION_HEADER_OFFSET = 0x28
private const val SECTION_HEADER_SIZE = 0x3a
private const val SECTION_HEADER_COUNT = 0x3c
private const val PROGRAM_HEADER_OFFSET = 0x20
private const val PROGRAM_HEADER_SIZE = 0x36
private const val PROGRAM_HEADER_COUNT = 0x38

private const val SECTION_TYPE = 0x04
private const val SECTION_OFFSET = 0x18
private const val SECTION_SIZE = 0x20
private const val SECTION_LINK = 0x28
private const val SECTION_ENTRY_SIZE = 0x38
private const val SECTION_TYPE_DYNSYM = 11

private const val SYMBOL_NAME = 0x00
private const val SYMBOL_VALUE = 0x08

private const val PROGRAM_TYPE = 0x00
private const val PROGRAM_OFFSET = 0x08
private const val PROGRAM_VIRTUAL_ADDRESS = 0x10
private const val PROGRAM_FILE_SIZE = 0x20
private const val PROGRAM_TYPE_LOAD = 1

private const val FUNCTION_SCAN_LIMIT = 128

internal class ElfImage(private val bytes: ByteArray) {

    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    fun symbol(name: String): Long {
        val sections = (0 until buffer.getShort(SECTION_HEADER_COUNT).toInt()).map { index ->
            buffer.getLong(SECTION_HEADER_OFFSET).toInt() + index * buffer.getShort(SECTION_HEADER_SIZE)
        }
        val dynsym = sections.firstOrNull { buffer.getInt(it + SECTION_TYPE) == SECTION_TYPE_DYNSYM }
            ?: throw PatchException("No dynamic symbol table")
        val strings = sections[buffer.getInt(dynsym + SECTION_LINK)]
        val stringsOffset = buffer.getLong(strings + SECTION_OFFSET).toInt()
        val entrySize = buffer.getLong(dynsym + SECTION_ENTRY_SIZE).toInt()
        val tableOffset = buffer.getLong(dynsym + SECTION_OFFSET).toInt()
        val count = buffer.getLong(dynsym + SECTION_SIZE).toInt() / entrySize
        for (index in 0 until count) {
            val entry = tableOffset + index * entrySize
            if (cString(stringsOffset + buffer.getInt(entry + SYMBOL_NAME)) == name) {
                return buffer.getLong(entry + SYMBOL_VALUE)
            }
        }
        throw PatchException("Symbol $name is missing")
    }

    fun pageRelativeLoads(function: Long): List<Long> {
        val pages = HashMap<Int, Long>()
        val loads = mutableListOf<Long>()
        for (index in 0 until FUNCTION_SCAN_LIMIT) {
            val address = function + index * Arm64.INSTRUCTION_BYTES
            val instruction = buffer.getInt(fileOffset(address))
            when {
                Arm64.isReturn(instruction) -> break
                Arm64.isAdrp(instruction) -> pages[Arm64.destination(instruction)] = Arm64.adrpPage(instruction, address)
                Arm64.isLoad64(instruction) -> pages[Arm64.baseRegister(instruction)]?.let { page ->
                    loads += page + Arm64.load64Offset(instruction)
                }
            }
        }
        return loads
    }

    fun readQword(address: Long): ULong = buffer.getLong(fileOffset(address)).toULong()

    private fun fileOffset(address: Long): Int {
        val programs = buffer.getLong(PROGRAM_HEADER_OFFSET).toInt()
        for (index in 0 until buffer.getShort(PROGRAM_HEADER_COUNT)) {
            val header = programs + index * buffer.getShort(PROGRAM_HEADER_SIZE)
            if (buffer.getInt(header + PROGRAM_TYPE) != PROGRAM_TYPE_LOAD) continue
            val start = buffer.getLong(header + PROGRAM_VIRTUAL_ADDRESS)
            if (address >= start && address < start + buffer.getLong(header + PROGRAM_FILE_SIZE)) {
                return (buffer.getLong(header + PROGRAM_OFFSET) + address - start).toInt()
            }
        }
        throw PatchException("Address 0x${address.toString(16)} is outside every loaded segment")
    }

    private fun cString(offset: Int): String {
        var end = offset
        while (bytes[end] != 0.toByte()) end++
        return String(bytes, offset, end - offset, Charsets.US_ASCII)
    }
}

internal object Arm64 {
    const val INSTRUCTION_BYTES = 4

    private const val REGISTER_MASK = 0x1f
    private const val BASE_REGISTER_SHIFT = 5
    private const val RETURN = 0xd65f03c0.toInt()

    private const val ADRP_MASK = 0x9f000000.toInt()
    private const val ADRP = 0x90000000.toInt()
    private const val ADRP_LOW_SHIFT = 29
    private const val ADRP_LOW_MASK = 0x3
    private const val ADRP_HIGH_SHIFT = 5
    private const val ADRP_HIGH_MASK = 0x7ffff
    private const val ADRP_HIGH_BITS = 2
    private const val ADRP_IMMEDIATE_BITS = 21
    private const val PAGE_BITS = 12

    private const val LOAD64_MASK = 0xffc00000.toInt()
    private const val LOAD64 = 0xf9400000.toInt()
    private const val LOAD64_IMMEDIATE_SHIFT = 10
    private const val LOAD64_IMMEDIATE_MASK = 0xfff
    private const val LOAD64_SCALE = 8

    private const val BRANCH_LINK_MASK = 0xfc000000.toInt()
    private const val BRANCH_LINK = 0x94000000.toInt()
    private const val BRANCH = 0x14000000
    private const val BRANCH_IMMEDIATE_BITS = 26
    private const val BRANCH_IMMEDIATE_MASK = (1 shl BRANCH_IMMEDIATE_BITS) - 1

    private const val TEST_BRANCH_ZERO_MASK = 0x7f000000
    private const val TEST_BRANCH_ZERO = 0x36000000
    private const val TEST_BIT_HIGH_SHIFT = 31
    private const val TEST_BIT_LOW_SHIFT = 19
    private const val TEST_BIT_LOW_MASK = 0x1f
    private const val TEST_BIT_HIGH_POSITION = 5
    private const val TEST_IMMEDIATE_SHIFT = 5
    private const val TEST_IMMEDIATE_BITS = 14

    private const val MOVE_WIDE_MASK = 0xffe00000.toInt()
    private const val MOVE_WIDE_ZERO_W = 0x52800000
    private const val MOVE_WIDE_IMMEDIATE_SHIFT = 5
    private const val MOVE_WIDE_IMMEDIATE_MASK = 0xffff

    fun isReturn(instruction: Int) = instruction == RETURN

    fun isAdrp(instruction: Int) = instruction and ADRP_MASK == ADRP

    fun isLoad64(instruction: Int) = instruction and LOAD64_MASK == LOAD64

    fun isBranchLink(instruction: Int) = instruction and BRANCH_LINK_MASK == BRANCH_LINK

    fun isTestBitZeroBranch(instruction: Int, bit: Int): Boolean {
        val testedBit = ((instruction ushr TEST_BIT_HIGH_SHIFT) shl TEST_BIT_HIGH_POSITION) or
            ((instruction ushr TEST_BIT_LOW_SHIFT) and TEST_BIT_LOW_MASK)
        return instruction and TEST_BRANCH_ZERO_MASK == TEST_BRANCH_ZERO && testedBit == bit
    }

    fun isMoveWideZeroW(instruction: Int, immediate: Int) = instruction and MOVE_WIDE_MASK == MOVE_WIDE_ZERO_W &&
        (instruction ushr MOVE_WIDE_IMMEDIATE_SHIFT) and MOVE_WIDE_IMMEDIATE_MASK == immediate

    fun destination(instruction: Int) = instruction and REGISTER_MASK

    fun baseRegister(instruction: Int) = (instruction ushr BASE_REGISTER_SHIFT) and REGISTER_MASK

    fun adrpPage(instruction: Int, address: Long): Long {
        val low = (instruction ushr ADRP_LOW_SHIFT) and ADRP_LOW_MASK
        val high = (instruction ushr ADRP_HIGH_SHIFT) and ADRP_HIGH_MASK
        val immediate = signExtend((high shl ADRP_HIGH_BITS) or low, ADRP_IMMEDIATE_BITS).toLong()
        return (address shr PAGE_BITS shl PAGE_BITS) + (immediate shl PAGE_BITS)
    }

    fun load64Offset(instruction: Int) =
        ((instruction ushr LOAD64_IMMEDIATE_SHIFT) and LOAD64_IMMEDIATE_MASK).toLong() * LOAD64_SCALE

    fun branchLinkTarget(instruction: Int, address: Int) =
        address + signExtend(instruction and BRANCH_IMMEDIATE_MASK, BRANCH_IMMEDIATE_BITS) * INSTRUCTION_BYTES

    fun testBranchOffsetWords(instruction: Int) =
        signExtend((instruction ushr TEST_IMMEDIATE_SHIFT) and ((1 shl TEST_IMMEDIATE_BITS) - 1), TEST_IMMEDIATE_BITS)

    fun branch(offsetWords: Int) = BRANCH or (offsetWords and BRANCH_IMMEDIATE_MASK)

    fun branchLink(offsetWords: Int) = BRANCH_LINK or (offsetWords and BRANCH_IMMEDIATE_MASK)

    private fun signExtend(value: Int, bits: Int) = (value shl (Int.SIZE_BITS - bits)) shr (Int.SIZE_BITS - bits)
}
