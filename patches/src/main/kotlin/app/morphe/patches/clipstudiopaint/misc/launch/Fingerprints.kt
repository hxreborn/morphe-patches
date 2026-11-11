/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.launch

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.netanalyzer.shared.Arm64
import app.morphe.patches.netanalyzer.shared.Arm64.INSTRUCTION_BYTES
import app.morphe.patches.shared.misc.arm64.arm64SecondLoadRegister
import app.morphe.patches.shared.misc.arm64.writesArm64GeneralRegister
import java.nio.ByteBuffer

private const val SECTION_TABLE = 0x28
private const val SECTION_ENTRY_SIZE = 0x3a
private const val SECTION_COUNT = 0x3c
private const val SECTION_NAMES_INDEX = 0x3e
private const val SECTION_ADDRESS = 0x10
private const val SECTION_FILE_OFFSET = 0x18
private const val SECTION_SIZE = 0x20
private const val ADDRESS_REFERENCE_WORDS = 8
private const val DESCRIPTOR_INITIALIZER_WORDS = 8
private const val REGISTER_MASK = 31
private const val IMMEDIATE_SHIFT = 10
private const val ADD_MASK = 0xffc00000.toInt()
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_IMMEDIATE_MASK = 0xfff
private const val PAGE_MASK = -0x1000L
private const val STORE_PAIR_MASK = 0xffc00000.toInt()
private const val STORE_PAIR = 0xa9000000.toInt()
private const val SECOND_REGISTER_SHIFT = 10
private const val PAIR_OFFSET_SHIFT = 15
private const val PAIR_OFFSET_MASK = 127
private const val DESCRIPTOR_POINTERS_OFFSET = Long.SIZE_BYTES
private const val BRANCH_MASK = 0xfc000000.toInt()
private const val BRANCH = 0x14000000
private const val CONDITIONAL_BRANCH_MASK = 0xff00001f.toInt()
private const val BRANCH_NOT_EQUAL = 0x54000001
private const val CONDITIONAL_IMMEDIATE_SHIFT = 5
private const val CONDITIONAL_IMMEDIATE_BITS = 19
private const val COMPARE_RETURN_MASK = 0xfffffc1f.toInt()
private const val COMPARE_RETURN = 0x6b00001f
private const val MOVE_DEFAULT_PAGE = 0x12800000
private const val MOVE_NO_FLAGS = 0x2a1f03e1
private const val SHOW_HOME_IF_NO_DRAWING_EVENT = "applicationshowicewindowifnoworkview"

private data class NativeSection(val fileOffset: Int, val address: Long, val sizeBytes: Int)
private data class AddressReference(val adrpOffset: Int, val addOffset: Int, val destinationRegister: Int)

internal object StartupHomeFingerprint {
    fun match(buffer: ByteBuffer): Int {
        val sectionTableOffset = buffer.getLong(SECTION_TABLE).toInt()
        val sectionHeaderBytes = buffer.getShort(SECTION_ENTRY_SIZE).toInt()
        val sectionHeaderOffsets = (0 until buffer.getShort(SECTION_COUNT)).map { sectionTableOffset + it * sectionHeaderBytes }
        val sectionNamesOffset = buffer.getLong(sectionHeaderOffsets[buffer.getShort(SECTION_NAMES_INDEX).toInt()] + SECTION_FILE_OFFSET).toInt()
        fun findSection(name: String): NativeSection {
            val header = sectionHeaderOffsets.single { buffer.stringAt(sectionNamesOffset + buffer.getInt(it)) == name }
            return NativeSection(
                buffer.getLong(header + SECTION_FILE_OFFSET).toInt(),
                buffer.getLong(header + SECTION_ADDRESS),
                buffer.getLong(header + SECTION_SIZE).toInt(),
            )
        }
        val code = findSection(".text")
        val strings = findSection(".rodata")
        val literal = (SHOW_HOME_IF_NO_DRAWING_EVENT + '\u0000').toByteArray(Charsets.US_ASCII)
        val stringOffsets = (strings.fileOffset..strings.fileOffset + strings.sizeBytes - literal.size).filter { offset ->
            buffer.get(offset) == literal.first() && literal.indices.all { buffer.get(offset + it) == literal[it] }
        }
        val stringOffset = unique("home event name", stringOffsets)
        val stringAddress = strings.address + stringOffset - strings.fileOffset
        val descriptorAddresses = findAddressReferences(buffer, code, stringAddress).flatMap { reference ->
            val registers = mutableMapOf<Int, Long>()
            val initializerAddresses = mutableListOf<Long>()
            val start = maxOf(code.fileOffset, reference.adrpOffset - DESCRIPTOR_INITIALIZER_WORDS * INSTRUCTION_BYTES)
            val end = minOf(code.fileOffset + code.sizeBytes, reference.addOffset + DESCRIPTOR_INITIALIZER_WORDS * INSTRUCTION_BYTES)
            for (offset in start until end step INSTRUCTION_BYTES) {
                val word = buffer.getInt(offset)
                val destination = Arm64.destination(word)
                val base = Arm64.baseRegister(word)
                when {
                    Arm64.isAdrp(word) -> registers[destination] = Arm64.adrpPage(word, code.address + offset - code.fileOffset)
                    word and ADD_MASK == ADD_IMMEDIATE -> {
                        val value = registers[base]?.plus((word ushr IMMEDIATE_SHIFT and ADD_IMMEDIATE_MASK).toLong())
                        registers.remove(destination)
                        if (value != null) registers[destination] = value
                    }
                    word and STORE_PAIR_MASK == STORE_PAIR &&
                        word ushr SECOND_REGISTER_SHIFT and REGISTER_MASK == reference.destinationRegister &&
                        (word ushr PAIR_OFFSET_SHIFT and PAIR_OFFSET_MASK) * Long.SIZE_BYTES == DESCRIPTOR_POINTERS_OFFSET &&
                        registers[reference.destinationRegister] == stringAddress -> registers[base]?.let(initializerAddresses::add)
                    Arm64.isBranchLink(word) -> registers.clear()
                    else -> {
                        if (writesArm64GeneralRegister(word)) registers.remove(destination)
                        arm64SecondLoadRegister(word)?.let(registers::remove)
                    }
                }
            }
            initializerAddresses
        }
        val descriptorAddress = unique("home event descriptor", descriptorAddresses)
        val homeCallOffsets = findAddressReferences(buffer, code, descriptorAddress).flatMap { reference ->
            val resolverCallOffset = reference.addOffset + INSTRUCTION_BYTES
            val compareOffset = resolverCallOffset + INSTRUCTION_BYTES
            val mismatchBranchOffset = compareOffset + INSTRUCTION_BYTES
            if (!Arm64.isBranchLink(buffer.getInt(resolverCallOffset)) ||
                buffer.getInt(compareOffset) and COMPARE_RETURN_MASK != COMPARE_RETURN ||
                buffer.getInt(mismatchBranchOffset) and CONDITIONAL_BRANCH_MASK != BRANCH_NOT_EQUAL
            ) return@flatMap emptyList()
            val branchOffsetWords = buffer.getInt(mismatchBranchOffset) shl (Int.SIZE_BITS - CONDITIONAL_IMMEDIATE_SHIFT - CONDITIONAL_IMMEDIATE_BITS) shr
                (Int.SIZE_BITS - CONDITIONAL_IMMEDIATE_BITS)
            val handlerEndOffset = mismatchBranchOffset + branchOffsetWords * INSTRUCTION_BYTES
            if (handlerEndOffset <= mismatchBranchOffset || handlerEndOffset > code.fileOffset + code.sizeBytes) return@flatMap emptyList()
            (mismatchBranchOffset + INSTRUCTION_BYTES until handlerEndOffset - 3 * INSTRUCTION_BYTES step INSTRUCTION_BYTES).filter { offset ->
                buffer.getInt(offset) == MOVE_DEFAULT_PAGE &&
                    buffer.getInt(offset + INSTRUCTION_BYTES) == MOVE_NO_FLAGS &&
                    Arm64.isBranchLink(buffer.getInt(offset + 2 * INSTRUCTION_BYTES)) &&
                    buffer.getInt(offset + 3 * INSTRUCTION_BYTES) and BRANCH_MASK == BRANCH
            }.map { it + 2 * INSTRUCTION_BYTES }
        }
        return unique("automatic home display", homeCallOffsets)
    }

    private fun findAddressReferences(buffer: ByteBuffer, code: NativeSection, target: Long): List<AddressReference> {
        val found = mutableListOf<AddressReference>()
        for (offset in code.fileOffset until code.fileOffset + code.sizeBytes step INSTRUCTION_BYTES) {
            val word = buffer.getInt(offset)
            if (!Arm64.isAdrp(word)) continue
            val address = code.address + offset - code.fileOffset
            val page = Arm64.adrpPage(word, address)
            if (page != (target and PAGE_MASK)) continue
            val register = Arm64.destination(word)
            val end = minOf(code.fileOffset + code.sizeBytes, offset + (ADDRESS_REFERENCE_WORDS + 1) * INSTRUCTION_BYTES)
            for (next in offset + INSTRUCTION_BYTES until end step INSTRUCTION_BYTES) {
                val instruction = buffer.getInt(next)
                if (instruction and ADD_MASK == ADD_IMMEDIATE && Arm64.baseRegister(instruction) == register) {
                    if (page + (instruction ushr IMMEDIATE_SHIFT and ADD_IMMEDIATE_MASK) == target) {
                        found += AddressReference(offset, next, Arm64.destination(instruction))
                    }
                    break
                }
                if (writesArm64GeneralRegister(instruction) && Arm64.destination(instruction) == register ||
                    arm64SecondLoadRegister(instruction) == register || Arm64.isBranchLink(instruction)
                ) break
            }
        }
        return found
    }

    private fun ByteBuffer.stringAt(offset: Int): String = buildString {
        var cursor = offset
        while (this@stringAt.get(cursor) != 0.toByte()) append(this@stringAt.get(cursor++).toInt().toChar())
    }

    private fun <T> unique(name: String, matches: List<T>): T = matches.singleOrNull()
        ?: throw PatchException("Expected one $name in Clip Studio Paint, found ${matches.size}")
}
