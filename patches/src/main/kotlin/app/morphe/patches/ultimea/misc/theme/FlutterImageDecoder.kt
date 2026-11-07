/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.indexOfMasked
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ELF_PROGRAM_OFFSET = 32
private const val ELF_PROGRAM_ENTRY_BYTES = 54
private const val ELF_PROGRAM_COUNT = 56
private const val LOAD_SEGMENT = 1
private const val EXECUTABLE_SEGMENT = 1
private const val SEGMENT_FLAGS = 4
private const val SEGMENT_OFFSET = 8
private const val SEGMENT_ADDRESS = 16
private const val SEGMENT_SIZE = 32
private const val HALFWORD_MASK = 0xffff
private const val INSTRUCTION_BYTES = 4
private const val RELOCATION_BYTES = 24
private const val POINTER_BYTES = 8
private const val RELATIVE_RELOCATION = 1027L
private const val CALLBACK_ENTRY_WINDOW = 128
private const val VTABLE_WINDOW = 64L
private const val REGISTRATION_WINDOW = 192
private const val ADDRESS_PAIR_WINDOW = 24
private const val REGISTER_MASK = 31
private const val REGISTER_SOURCE_SHIFT = 5
private const val PAGE_MASK = 0x9f000000.toInt()
private const val PAGE_ADDRESS = 0x90000000.toInt()
private const val PAGE_IMMEDIATE_LOW_SHIFT = 29
private const val PAGE_IMMEDIATE_HIGH_MASK = 0x7ffff
private const val PAGE_IMMEDIATE_SIGN_SHIFT = 11
private const val PAGE_SHIFT = 12
private const val PAGE_OFFSET_MASK = 0xfff
private const val ADD_IMMEDIATE_MASK = 0xffc00000.toInt()
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_IMMEDIATE_SHIFT = 10
private const val SUB_STACK_MASK = 0xffc003ff.toInt()
private const val SUB_STACK = 0xd10003ff.toInt()
private const val NEGATIVE_ONE_MASK = 0xffffffe0.toInt()
private const val NEGATIVE_ONE = 0x12800000
private const val PRIORITY_ONE = 0x52800020
private const val STACK_STORE_MASK = 0xffc003e0.toInt()
private const val STACK_STORE = 0xb90003e0.toInt()
private const val PRIORITY_STORE_DISTANCE = 8
private const val WORKER_PRIORITY_MESSAGE = "Failed to set Workers task runner priority"

internal object FlutterImageDecoder {
    private data class Segment(val offset: Int, val address: Long, val size: Int, val flags: Int)

    fun apply(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val site = priorityInstruction(bytes)
        buffer.putInt(site, PRIORITY_ONE or (buffer.getInt(site) and REGISTER_MASK))
    }

    fun priorityInstruction(bytes: ByteArray): Int {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val programs = buffer.getLong(ELF_PROGRAM_OFFSET).toInt()
        val entrySize = buffer.getShort(ELF_PROGRAM_ENTRY_BYTES).toInt() and HALFWORD_MASK
        val count = buffer.getShort(ELF_PROGRAM_COUNT).toInt() and HALFWORD_MASK
        val segments = (0 until count).mapNotNull { index ->
            val header = programs + index * entrySize
            if (buffer.getInt(header) != LOAD_SEGMENT) return@mapNotNull null
            Segment(buffer.getLong(header + SEGMENT_OFFSET).toInt(), buffer.getLong(header + SEGMENT_ADDRESS),
                buffer.getLong(header + SEGMENT_SIZE).toInt(), buffer.getInt(header + SEGMENT_FLAGS))
        }
        val code = segments.single { it.flags and EXECUTABLE_SEGMENT != 0 }
        fun address(offset: Int) = segments.single { offset in it.offset until it.offset + it.size }
            .let { it.address + offset - it.offset }
        val message = WORKER_PRIORITY_MESSAGE.toByteArray()
        val mask = ByteArray(message.size) { -1 }
        val messageOffset = bytes.indexOfMasked(message, mask)
        if (messageOffset < 0 || bytes.indexOfMasked(message, mask, messageOffset + message.size) >= 0) {
            throw PatchException("Expected one Flutter worker priority message")
        }
        val references = mutableListOf<Pair<Int, Long>>()
        for (site in code.offset until code.offset + code.size - ADDRESS_PAIR_WINDOW step INSTRUCTION_BYTES) {
            val instruction = buffer.getInt(site)
            if (instruction and PAGE_MASK != PAGE_ADDRESS) continue
            val encodedPage = ((instruction ushr REGISTER_SOURCE_SHIFT and PAGE_IMMEDIATE_HIGH_MASK) shl 2) or
                (instruction ushr PAGE_IMMEDIATE_LOW_SHIFT and 3)
            val page = (address(site) and PAGE_OFFSET_MASK.toLong().inv()) +
                ((encodedPage shl PAGE_IMMEDIATE_SIGN_SHIFT shr PAGE_IMMEDIATE_SIGN_SHIFT).toLong() shl PAGE_SHIFT)
            for (next in site + INSTRUCTION_BYTES..site + ADDRESS_PAIR_WINDOW step INSTRUCTION_BYTES) {
                val add = buffer.getInt(next)
                if (add and ADD_IMMEDIATE_MASK == ADD_IMMEDIATE &&
                    add ushr REGISTER_SOURCE_SHIFT and REGISTER_MASK == instruction and REGISTER_MASK
                ) {
                    references += site to page + (add ushr ADD_IMMEDIATE_SHIFT and PAGE_OFFSET_MASK)
                }
            }
        }
        val messageReference = references.filter { it.second == address(messageOffset) }.map { it.first }.distinct()
            .singleOrNull() ?: throw PatchException("Expected one Flutter worker priority reference")
        val callback = (messageReference downTo messageReference - CALLBACK_ENTRY_WINDOW step INSTRUCTION_BYTES)
            .firstOrNull { buffer.getInt(it) and SUB_STACK_MASK == SUB_STACK }
            ?: throw PatchException("Could not find the Flutter worker callback")
        val callbackSlots = (RELOCATION_BYTES until bytes.size - POINTER_BYTES step POINTER_BYTES).mapNotNull { position ->
            if (buffer.getLong(position) != address(callback) || buffer.getLong(position - POINTER_BYTES) != RELATIVE_RELOCATION) null
            else buffer.getLong(position - POINTER_BYTES * 2)
        }
        val callbackSlot = callbackSlots.singleOrNull() ?: throw PatchException("Expected one Flutter worker callback relocation")
        val registrations = references.filter { it.second in callbackSlot - VTABLE_WINDOW..callbackSlot }.flatMap { (site, _) ->
            (site until site + REGISTRATION_WINDOW step INSTRUCTION_BYTES).filter { candidate ->
                val priority = buffer.getInt(candidate)
                val store = buffer.getInt(candidate + PRIORITY_STORE_DISTANCE)
                priority and NEGATIVE_ONE_MASK == NEGATIVE_ONE && store and STACK_STORE_MASK == STACK_STORE &&
                    priority and REGISTER_MASK == store and REGISTER_MASK
            }
        }.distinct()
        return registrations.singleOrNull() ?: throw PatchException("Expected one Android image decoder registration priority")
    }
}
