/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt

private const val INSTRUCTION_BYTES = 4
private const val FRAME_ENTRY = 0xa9bf79fd.toInt()
private const val RETURN = 0xd65f03c0.toInt()
private const val FRAME_POINTER = 29
private const val RETURN_REGISTER = 0
private const val CALLER_SAVED_REGISTERS = 18
private const val REGISTER_MASK = 31
private const val SECOND_REGISTER_SHIFT = 5
private const val THIRD_REGISTER_SHIFT = 16
private const val DIRECT_CALL_MASK = 0xfc000000.toInt()
private const val DIRECT_CALL = 0x94000000.toInt()
private const val DIRECT_BRANCH = 0x14000000
private const val INDIRECT_CALL_MASK = 0xfffffc1f.toInt()
private const val INDIRECT_CALL = 0xd63f0000.toInt()
private const val BRANCH_SIGN_SHIFT = 6
private const val BRANCH_SCALE_SHIFT = 2
private const val MOVE_REGISTER_MASK = 0xffe0ffe0.toInt()
private const val MOVE_REGISTER = 0xaa0003e0.toInt()
private const val MEMORY_MASK = 0xffe00c00.toInt()
private const val LOAD_DOUBLE = 0xfc400000.toInt()
private const val LOAD_REGISTER = 0xf8400000.toInt()
private const val STORE_REGISTER = 0xf8000000.toInt()
private const val STORE_SINGLE = 0xbc000000.toInt()
private const val MEMORY_SIGN_SHIFT = 11
private const val MEMORY_OFFSET_SHIFT = 12
private const val INDEXED_STORE_MASK = 0xffe0fc00.toInt()
private const val STORE_SINGLE_INDEXED = 0xbc206800.toInt()
private const val STORE_WORD_INDEXED = 0xb8206800.toInt()
private const val FLOAT_CONVERSION_MASK = 0xfffffc00.toInt()
private const val DOUBLE_TO_SINGLE = 0x1e624000
private const val FLOAT_SUBTRACT_MASK = 0xffe0fc00.toInt()
private const val FLOAT_SUBTRACT = 0x1e603800
private const val ADD_IMMEDIATE_MASK = 0xffc00000.toInt()
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_IMMEDIATE_SHIFT = 10
private const val ADD_IMMEDIATE_VALUE_MASK = 0xfff
private const val BITFIELD_MASK = 0xffc00000.toInt()
private const val UNSIGNED_BITFIELD = 0xd3400000.toInt()
private const val BITFIELD_VALUE_MASK = 63
private const val BITFIELD_END_SHIFT = 10
private const val OR_REGISTER = 0xaa000000.toInt()
private const val PACKED_COMPONENT_COUNT = 4
private const val COLOR_COMPONENT_BYTES = 8
private const val FIRST_COLOR_COMPONENT = 7
private const val PAINT_COMPONENT_BYTES = 4
private const val PAINT_COLOR_SPACE_OFFSET = 20
private const val BUFFER_POINTER_OFFSET = 7
private const val TYPED_DATA_PAYLOAD_OFFSET = 23
private const val FRAME_LEAVE_INSTRUCTIONS = 3
private const val MAX_COLOR_METHOD_BYTES = 1024

internal data class AccentInstructionMatches(
    val paintColorReturn: Int,
    val packedColorReturn: Int,
    val gradientColorReturn: Int,
    val paintBufferRegister: Int,
    val paintOffsetRegister: Int,
    val packedColorRegister: Int,
)

internal class AccentInstructions(
    private val bytes: ByteArray,
    private val codeStart: Int,
    private val codeEnd: Int,
) {
    private data class PackedValue(val channels: Map<Int, Int>)

    private data class PaintOffset(val slot: Int, val bytes: Int)

    private data class PaintMatch(val position: Int, val bufferRegister: Int, val offsetRegister: Int)

    private data class PackedMatch(val position: Int, val colorRegister: Int)

    fun match(): AccentInstructionMatches {
        val paint = mutableListOf<PaintMatch>()
        val packed = mutableListOf<PackedMatch>()
        val gradients = mutableListOf<Int>()
        for (entry in codeStart until codeEnd step INSTRUCTION_BYTES) {
            if (word(entry) != FRAME_ENTRY) continue
            val end = firstReturn(entry) ?: continue
            paintMatch(entry, end)?.let(paint::add)
            packedMatch(entry, end)?.let(packed::add)
            gradientMatch(entry, end)?.let(gradients::add)
        }
        val paintMatch = paint.singleOrNull()
            ?: throw PatchException("Expected one Paint color serializer, found ${paint.size}")
        val packedMatch = packed.singleOrNull()
            ?: throw PatchException("Expected one packed Color converter, found ${packed.size}")
        val gradient = gradients.singleOrNull()
            ?: throw PatchException("Expected one gradient Color converter, found ${gradients.size}")
        return AccentInstructionMatches(
            paintMatch.position,
            packedMatch.position,
            gradient,
            paintMatch.bufferRegister,
            paintMatch.offsetRegister,
            packedMatch.colorRegister,
        )
    }

    private fun paintMatch(entry: Int, end: Int): PaintMatch? {
        val slots = mutableMapOf<Int, Int>()
        val offsets = mutableMapOf<Int, PaintOffset>()
        val conversions = mutableSetOf<Int>()
        val stores = mutableListOf<PaintOffset>()
        var alphaDifferenceRegister = -1
        var inverseAlpha = false
        var bufferSlot: Int? = null
        var finalStore = -1
        var bufferRegister = -1
        var offsetRegister = -1
        var conversionCount = 0
        for (position in entry until end step INSTRUCTION_BYTES) {
            val instruction = word(position)
            val destination = destination(instruction)
            val source = source(instruction)
            when {
                instruction and DIRECT_CALL_MASK == DIRECT_CALL ||
                    instruction and INDIRECT_CALL_MASK == INDIRECT_CALL -> {
                    for (register in 0 until CALLER_SAVED_REGISTERS) {
                        slots.remove(register)
                        offsets.remove(register)
                    }
                    conversions.clear()
                }
                instruction and MEMORY_MASK == LOAD_REGISTER -> {
                    offsets.remove(destination)
                    if (source == FRAME_POINTER) slots[destination] = memoryOffset(instruction)
                    else slots.remove(destination)
                }
                instruction and MEMORY_MASK == STORE_REGISTER && source == FRAME_POINTER ->
                    slots[destination] = memoryOffset(instruction)
                instruction and ADD_IMMEDIATE_MASK == ADD_IMMEDIATE -> {
                    val slot = slots[source]
                    slots.remove(destination)
                    offsets.remove(destination)
                    if (slot != null) offsets[destination] = PaintOffset(slot, immediate(instruction))
                }
                instruction and FLOAT_CONVERSION_MASK == DOUBLE_TO_SINGLE -> {
                    conversions += destination
                    conversionCount++
                    if (conversionCount == PACKED_COMPONENT_COUNT) inverseAlpha = source == alphaDifferenceRegister
                }
                instruction and FLOAT_SUBTRACT_MASK == FLOAT_SUBTRACT -> alphaDifferenceRegister = destination
                instruction and INDEXED_STORE_MASK == STORE_SINGLE_INDEXED ||
                    instruction and INDEXED_STORE_MASK == STORE_WORD_INDEXED -> {
                    val indexedOffsetRegister = third(instruction)
                    val offset = offsets[indexedOffsetRegister] ?: return null
                    val pointerLoad = word(position - INSTRUCTION_BYTES)
                    if (pointerLoad and MEMORY_MASK != LOAD_REGISTER ||
                        destination(pointerLoad) != source || memoryOffset(pointerLoad) != BUFFER_POINTER_OFFSET
                    ) return null
                    val slot = slots[source(pointerLoad)] ?: return null
                    if (bufferSlot != null && bufferSlot != slot) return null
                    bufferSlot = slot
                    if (instruction and INDEXED_STORE_MASK == STORE_SINGLE_INDEXED) {
                        if (destination !in conversions) return null
                        stores += offset
                    } else {
                        if (offset.bytes != PAINT_COLOR_SPACE_OFFSET || finalStore >= 0) return null
                        finalStore = position
                        bufferRegister = source
                        offsetRegister = indexedOffsetRegister
                        stores += offset
                    }
                }
            }
        }
        if (!inverseAlpha || conversionCount != PACKED_COMPONENT_COUNT || stores.size != PACKED_COMPONENT_COUNT + 1 ||
            stores.map { it.bytes } != (1..stores.size).map { it * PAINT_COMPONENT_BYTES } ||
            stores.map { it.slot }.distinct().size != 1
        ) return null
        val hook = end - FRAME_LEAVE_INSTRUCTIONS * INSTRUCTION_BYTES
        val instruction = word(hook)
        if (hook != finalStore + INSTRUCTION_BYTES || instruction and MOVE_REGISTER_MASK != MOVE_REGISTER ||
            destination(instruction) != RETURN_REGISTER
        ) return null
        return PaintMatch(hook, bufferRegister, offsetRegister)
    }

    private fun packedMatch(entry: Int, end: Int): PackedMatch? {
        val registers = mutableMapOf<Int, PackedValue>()
        val locals = mutableMapOf<Int, PackedValue>()
        val objectSlots = mutableMapOf<Int, Int>()
        var colorSlot: Int? = null
        var component = 0
        var helper = -1
        var channelRegister = -1
        var pendingComponent = -1
        for (position in entry until end step INSTRUCTION_BYTES) {
            val instruction = word(position)
            val destination = destination(instruction)
            val source = source(instruction)
            when {
                instruction and MEMORY_MASK == LOAD_DOUBLE -> {
                    if (component >= PACKED_COMPONENT_COUNT ||
                        memoryOffset(instruction) != FIRST_COLOR_COMPONENT + component * COLOR_COMPONENT_BYTES ||
                        word(position + INSTRUCTION_BYTES) and DIRECT_CALL_MASK != DIRECT_CALL
                    ) return null
                    val slot = objectSlots[source] ?: return null
                    if (colorSlot != null && colorSlot != slot) return null
                    colorSlot = slot
                    if (channelRegister >= 0 && channelRegister != destination) return null
                    channelRegister = destination
                    pendingComponent = component++
                }
                instruction and DIRECT_CALL_MASK == DIRECT_CALL -> {
                    if (pendingComponent < 0) return null
                    val target = branchTarget(position)
                    if (helper >= 0 && target != helper) return null
                    helper = target
                    for (register in 0 until CALLER_SAVED_REGISTERS) {
                        registers.remove(register)
                        objectSlots.remove(register)
                    }
                    registers[RETURN_REGISTER] = PackedValue(mapOf(pendingComponent to 0))
                    pendingComponent = -1
                }
                instruction and MOVE_REGISTER_MASK == MOVE_REGISTER ->
                    assign(registers, destination, registers[third(instruction)])
                instruction and MEMORY_MASK == STORE_REGISTER && source == FRAME_POINTER -> {
                    assign(locals, memoryOffset(instruction), registers[destination])
                    objectSlots[destination] = memoryOffset(instruction)
                }
                instruction and MEMORY_MASK == LOAD_REGISTER -> {
                    assign(registers, destination, if (source == FRAME_POINTER) locals[memoryOffset(instruction)] else null)
                    assign(objectSlots, destination, if (source == FRAME_POINTER) memoryOffset(instruction) else null)
                }
                instruction and BITFIELD_MASK == UNSIGNED_BITFIELD -> {
                    val start = instruction ushr THIRD_REGISTER_SHIFT and BITFIELD_VALUE_MASK
                    val last = instruction ushr BITFIELD_END_SHIFT and BITFIELD_VALUE_MASK
                    if (last + 1 != start) return null
                    val shift = Long.SIZE_BITS - start
                    assign(registers, destination, registers[source]?.let { value ->
                        PackedValue(value.channels.mapValues { it.value + shift })
                    })
                }
                instruction and INDEXED_STORE_MASK == OR_REGISTER -> {
                    val left = registers[source] ?: return null
                    val right = registers[third(instruction)] ?: return null
                    if (left.channels.keys.any { it in right.channels }) return null
                    registers[destination] = PackedValue(left.channels + right.channels)
                }
            }
        }
        val expected = (0 until PACKED_COMPONENT_COUNT).associateWith {
            (PACKED_COMPONENT_COUNT - 1 - it) * Byte.SIZE_BITS
        }
        val hook = end - FRAME_LEAVE_INSTRUCTIONS * INSTRUCTION_BYTES
        val instruction = word(hook)
        if (component != PACKED_COMPONENT_COUNT || instruction and MOVE_REGISTER_MASK != MOVE_REGISTER ||
            destination(instruction) != RETURN_REGISTER || registers[RETURN_REGISTER]?.channels != expected
        ) return null
        return PackedMatch(hook, third(instruction))
    }

    private fun gradientMatch(entry: Int, end: Int): Int? {
        var conversions = 0
        var stores = 0
        var conversionRegister = -1
        var repeats = false
        for (position in entry until end step INSTRUCTION_BYTES) {
            val instruction = word(position)
            when {
                instruction and FLOAT_CONVERSION_MASK == DOUBLE_TO_SINGLE -> {
                    conversions++
                    conversionRegister = destination(instruction)
                }
                instruction and MEMORY_MASK == STORE_SINGLE -> {
                    if (memoryOffset(instruction) != TYPED_DATA_PAYLOAD_OFFSET ||
                        destination(instruction) != conversionRegister
                    ) return null
                    stores++
                    conversionRegister = -1
                }
                instruction and DIRECT_CALL_MASK == DIRECT_BRANCH && branchTarget(position) in entry until position ->
                    repeats = true
            }
        }
        val hook = end - (FRAME_LEAVE_INSTRUCTIONS - 1) * INSTRUCTION_BYTES
        val instruction = word(hook)
        val preceding = word(hook - INSTRUCTION_BYTES)
        if (conversions != PACKED_COMPONENT_COUNT || stores != PACKED_COMPONENT_COUNT || !repeats ||
            instruction and MOVE_REGISTER_MASK != MOVE_REGISTER || third(instruction) != FRAME_POINTER ||
            destination(instruction) != source(word(entry)) ||
            !(preceding and DIRECT_CALL_MASK == DIRECT_CALL ||
                preceding and MOVE_REGISTER_MASK == MOVE_REGISTER && destination(preceding) == RETURN_REGISTER)
        ) return null
        return hook
    }

    private fun <T> assign(values: MutableMap<Int, T>, register: Int, value: T?) {
        if (value == null) values.remove(register) else values[register] = value
    }

    private fun firstReturn(entry: Int): Int? {
        val limit = minOf(codeEnd, entry + MAX_COLOR_METHOD_BYTES)
        for (position in entry + INSTRUCTION_BYTES until limit step INSTRUCTION_BYTES) {
            val instruction = word(position)
            if (instruction == FRAME_ENTRY) return null
            if (instruction == RETURN) return position
        }
        return null
    }

    private fun word(position: Int) = bytes.readInt(position)

    private fun destination(instruction: Int) = instruction and REGISTER_MASK

    private fun source(instruction: Int) = instruction ushr SECOND_REGISTER_SHIFT and REGISTER_MASK

    private fun third(instruction: Int) = instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK

    private fun immediate(instruction: Int) = instruction ushr ADD_IMMEDIATE_SHIFT and ADD_IMMEDIATE_VALUE_MASK

    private fun memoryOffset(instruction: Int) =
        instruction shl MEMORY_SIGN_SHIFT shr MEMORY_SIGN_SHIFT shr MEMORY_OFFSET_SHIFT

    private fun branchTarget(position: Int) = position +
        ((word(position) shl BRANCH_SIGN_SHIFT) shr BRANCH_SIGN_SHIFT shl BRANCH_SCALE_SHIFT)
}
