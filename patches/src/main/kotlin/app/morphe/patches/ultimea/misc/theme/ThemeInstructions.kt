/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.misc.arm64.arm64CompareAndSwapResultRegister
import app.morphe.patches.shared.misc.arm64.arm64ExclusiveStoreStatusRegister
import app.morphe.patches.shared.misc.arm64.arm64SecondLoadRegister
import app.morphe.patches.shared.misc.arm64.writesArm64GeneralRegister

private const val INSTRUCTION_BYTES = 4
private const val REGISTER_MASK = 31
private const val REGISTER_COUNT = 32
private const val PAIR_STORE_MASK = 0x3a400000
private const val PAIR_STORE = 0x28000000
private const val SIMD_MEMORY_FLAG = 1 shl 26
private const val SINGLE_MEMORY_KIND_MASK = 0x3b000000
private const val UNSCALED_MEMORY_KIND = 0x38000000
private const val UNSIGNED_MEMORY_KIND = 0x39000000
private val SINGLE_MEMORY_KINDS = setOf(UNSCALED_MEMORY_KIND, UNSIGNED_MEMORY_KIND)
private const val MEMORY_LOAD_FLAG = 1 shl 22
private const val SIMD_QUAD_FLAG = 1 shl 23
private const val REGISTER_OFFSET_FLAG = 1 shl 21
private const val QUAD_MEMORY_WIDTH_SHIFT = 4
private const val EXCLUSIVE_MEMORY_MASK = 0x3f000000
private const val EXCLUSIVE_MEMORY = 0x08000000
private const val ATOMIC_UPDATE_MASK = 0x3b200c00
private const val ATOMIC_UPDATE = 0x38200000
private const val MEMORY_PAIR_SIZE_SHIFT = 30
private const val PAIR_OFFSET_SIGN_SHIFT = 10
private const val PAIR_OFFSET_VALUE_SHIFT = 25
private const val ZERO_REGISTER = 31
private val GRADIENT_UNSAFE_SOURCES = setOf(DART_STACK_POINTER, 16, 30, ZERO_REGISTER)
private const val RESTORE_STACK_POINTER = 0xaa1d03ef.toInt()
private const val RESTORE_FRAME_POINTER = 0xa8c179fd.toInt()
private const val SECOND_REGISTER_SHIFT = 5
private const val THIRD_REGISTER_SHIFT = 16
private const val FRAME_POINTER = 29
private const val DART_STACK_POINTER = 15
private const val HEAP_REGISTER = 28
private const val RETURN_REGISTER = 0
private const val RECEIVER_REGISTER = 1
private const val ALLOCATION_SCRATCH_REGISTERS = 18
private const val CLASS_ID_SHIFT = 12
private const val CLASS_ID_MASK = 0xfffff
private const val ALLOCATION_SIZE_SHIFT = 8
private const val ALLOCATION_SIZE_MASK = 15
private const val ALLOCATION_ALIGNMENT = 16
private const val FRAME_ENTRY = 0xa9bf79fd.toInt()
private const val RETURN = 0xd65f03c0.toInt()
private const val DIRECT_CALL_MASK = 0xfc000000.toInt()
private const val DIRECT_CALL = 0x94000000.toInt()
private const val INDIRECT_CALL_MASK = 0xfffffc1f.toInt()
private const val INDIRECT_CALL = 0xd63f0000.toInt()
private const val DIRECT_BRANCH = 0x14000000
private const val CONDITIONAL_BRANCH_MASK = 0xff000010.toInt()
private const val CONDITIONAL_BRANCH = 0x54000000
private const val COMPARE_BRANCH_MASK = 0x7e000000
private const val COMPARE_BRANCH = 0x34000000
private const val TEST_BRANCH = 0x36000000
private const val CONDITIONAL_SIGN_SHIFT = 8
private const val TEST_SIGN_SHIFT = 13
private const val CONDITIONAL_IMMEDIATE_SHIFT = 5
private const val BRANCH_IMMEDIATE_SHIFT = 6
private const val BRANCH_SCALE_SHIFT = 2
private const val ARITHMETIC_SHIFT_MASK = 0xffc0fc00.toInt()
private const val ARITHMETIC_SHIFT = 0x9340fc00.toInt()
private const val SHIFT_AMOUNT_MASK = 63
private val COLOR_COMPONENT_SHIFTS = listOf(24, 16, 8)
private const val FLOAT_TO_SINGLE_MASK = 0xfffffc00.toInt()
private const val FLOAT_TO_SINGLE = 0x1e624000
private const val UNSCALED_MEMORY_MASK = 0xffe00c00.toInt()
private const val STORE_SINGLE = 0xbc000000.toInt()
private const val TYPED_DATA_PAYLOAD_OFFSET = 23
private const val COLOR_COMPONENT_COUNT = 4
private const val CONSTRUCTOR_ENTRY_WINDOW = 128
private const val MOVE_REGISTER_MASK = 0xffe0ffe0.toInt()
private const val MOVE_REGISTER = 0xaa0003e0.toInt()
private const val ADD_SUB_ADDRESS_MASK = 0x9f000000.toInt()
private const val ADD_SUB_IMMEDIATE_ADDRESS = 0x91000000.toInt()
private const val ADD_SUB_REGISTER_ADDRESS = 0x8b000000.toInt()
private const val ADDRESS_SHIFT_FLAG = 1 shl 22
private const val ADDRESS_SHIFT_BITS = 12
private const val SUBTRACT_FLAG = 1 shl 30
private const val SINGLE_INDEX_MODE_MASK = 0x00000c00
private const val SINGLE_POST_INDEX = 0x00000400
private const val PAIR_INDEX_MODE_MASK = 0x01800000
private const val PAIR_POST_INDEX = 0x00800000
private const val MOVE_IMMEDIATE_MASK = 0xff800000.toInt()
private const val MOVE_ZERO = 0xd2800000.toInt()
private const val MOVE_KEEP = 0xf2800000.toInt()
private const val IMMEDIATE_HALFWORD_MASK = 0xffff
private const val IMMEDIATE_HALFWORD_SHIFT = 5
private const val HALFWORD_SHIFT = 16
private const val HALFWORD_SELECTOR_SHIFT = 21
private const val DECOMPRESS_POINTER_MASK = 0xffe0fc00.toInt()
private const val DECOMPRESS_POINTER = 0x8b008000.toInt()
private const val UNREGISTERED_COLOR = -1
private const val TEXT_STYLE_SIZE = 112
private const val DECORATION_SIZE = 48
private const val ICON_SIZE = 64
private const val BORDER_SIZE = 32
private const val TEXT_COLOR_OFFSET = 11
private const val BACKGROUND_COLOR_OFFSET = 7
private const val ICON_COLOR_OFFSET = 35
private const val DIVIDER_COLOR_OFFSET = 31
private const val SMALL_WIDGET_SIZE = 48
private const val MIN_TEXT_COLOR_SITES = 50
private const val MIN_BACKGROUND_COLOR_SITES = 50
private const val MIN_ICON_COLOR_SITES = 10
private const val MIN_DIVIDER_COLOR_SITES = 10
private const val SHADOW_BLUR_OFFSET = 15
private val CONTAINER_PROPERTY_FIELDS = setOf(11, 15, 19, 23, 27, 39, 43, 47, 51)
private const val CONTAINER_COLOR_OFFSET = 23
private const val SCAFFOLD_SIZE = 80
private const val APP_BAR_SIZE = 112
private const val APP_BAR_COLOR_OFFSET = 67
private const val DIALOG_THEME_SIZE = 240
private const val DIALOG_COLOR_OFFSET = 179
private const val BORDER_WIDTH_OFFSET = 11
private const val NAVIGATION_COLOR_OFFSET = 11
private const val NAVIGATION_SELECTED_COLOR_OFFSET = 23
private const val MIN_SCAFFOLD_SITES = 10
private const val MIN_APP_BAR_SITES = 2
private const val MIN_DIALOG_THEME_SITES = 3
private const val MIN_NAVIGATION_SITES = 3

internal enum class ThemeColorRole(val code: Int) {
    INK(1),
    SURFACE(2),
    LINE(3),
    PRESERVE(4),
    PAGE(6),
    FACTORY(7),
}

internal data class ThemeInstructionMatches(
    val colorConstructor: Int,
    val colorComponents: Int,
    val gradientReturn: Int,
    val containerColorStore: Int,
    val colorRolesByReturnAddress: Map<Int, ThemeColorRole>,
)

internal class ThemeInstructions(
    private val bytes: ByteArray,
    private val codeStart: Int,
    private val codeEnd: Int,
) {
    private data class Allocation(val classId: Int, val size: Int)

    private class NativeObject(val allocation: Allocation, caller: Int? = null, val multipleInstances: Boolean = false) {
        val colorReturnAddresses = mutableSetOf<Int>().apply { caller?.let(::add) }
    }

    private data class FrameSlot(val offset: Int, val byteCount: Int)

    private data class FrameValue(val objects: Set<NativeObject>, val frameOffsets: Set<Int?> = emptySet())

    private data class ObjectField(val receiver: NativeObject, val offset: Int, val byteCount: Int)

    private data class BranchState(
        val registers: Array<Set<NativeObject>>,
        val locals: Map<FrameSlot, FrameValue>,
        val fields: Map<ObjectField, Set<NativeObject>>,
        val frameOffsets: Map<Int, Int?>,
    )

    private data class ColorStore(val caller: Int, val allocation: Allocation, val field: Int)

    private val entries = (codeStart until codeEnd step INSTRUCTION_BYTES).filter { word(it) == FRAME_ENTRY }
    private val allocations = findAllocations()
    private val barrierWrappers = findBarrierWrappers()
    private val factoryReturnSites = mutableSetOf<Int>()
    private val stores = mutableSetOf<ColorStore>()
    private val floatingFields = mutableMapOf<Int, MutableSet<Int>>()

    fun match(): ThemeInstructionMatches {
        val components = (codeStart + INSTRUCTION_BYTES until codeEnd - COLOR_COMPONENT_SHIFTS.size * INSTRUCTION_BYTES step INSTRUCTION_BYTES)
            .filter { position ->
                val source = sourceRegister(word(position))
                source == RETURN_REGISTER &&
                    word(position - INSTRUCTION_BYTES) and DIRECT_CALL_MASK == DIRECT_CALL &&
                    COLOR_COMPONENT_SHIFTS.withIndex().all { (index, shift) ->
                        val instruction = word(position + index * INSTRUCTION_BYTES)
                        instruction and ARITHMETIC_SHIFT_MASK == ARITHMETIC_SHIFT &&
                            sourceRegister(instruction) == source &&
                            instruction ushr THIRD_REGISTER_SHIFT and SHIFT_AMOUNT_MASK == shift
                    }
            }.singleMatch("hex color component conversion")
        val constructor = entries.lastOrNull { it < components && components - it <= CONSTRUCTOR_ENTRY_WINDOW }
            ?: throw PatchException("Could not find the hex color constructor entry")
        val gradient = entries.mapNotNull { entry ->
            val end = firstReturn(entry) ?: return@mapNotNull null
            val instructions = entry until end step INSTRUCTION_BYTES
            val stores = instructions.count {
                val instruction = word(it)
                instruction and UNSCALED_MEMORY_MASK == STORE_SINGLE &&
                    signedMemoryOffset(instruction) == TYPED_DATA_PAYLOAD_OFFSET
            }
            val conversions = instructions.count { word(it) and FLOAT_TO_SINGLE_MASK == FLOAT_TO_SINGLE }
            val repeats = instructions.any {
                val instruction = word(it)
                instruction and DIRECT_CALL_MASK == DIRECT_BRANCH && branchTarget(it) in entry until it
            }
            if (stores == COLOR_COMPONENT_COUNT && conversions == COLOR_COMPONENT_COUNT && repeats) end else null
        }.singleMatch("gradient color conversion")
        val gradientReturn = gradient - FRAME_LEAVE_BYTES
        val returnMove = word(gradientReturn)
        if (returnMove and MOVE_REGISTER_MASK != MOVE_REGISTER || destinationRegister(returnMove) != RETURN_REGISTER ||
            returnMove ushr THIRD_REGISTER_SHIFT and REGISTER_MASK in GRADIENT_UNSAFE_SOURCES ||
            word(gradientReturn + INSTRUCTION_BYTES) != RESTORE_STACK_POINTER ||
            word(gradientReturn + INSTRUCTION_BYTES * 2) != RESTORE_FRAME_POINTER) {
            throw PatchException("Unexpected gradient return instructions at $gradientReturn")
        }
        val containerStores = entries.flatMap { entry ->
            val end = firstReturn(entry) ?: return@flatMap emptyList()
            val stores = (entry until end step INSTRUCTION_BYTES).filter {
                word(it) and UNSCALED_MEMORY_MASK == 0xb8000000.toInt() && sourceRegister(word(it)) != FRAME_POINTER
            }.groupBy { sourceRegister(word(it)) }
            stores.values.filter { fields ->
                fields.size == CONTAINER_PROPERTY_FIELDS.size && fields.map { signedMemoryOffset(word(it)) }.toSet() == CONTAINER_PROPERTY_FIELDS
            }.flatMap { fields -> fields.filter { signedMemoryOffset(word(it)) == CONTAINER_COLOR_OFFSET } }
        }.singleMatch("container background assignment")
        val factories = mutableSetOf<Int>()
        do {
            val previousCount = factories.size
            collectColors(constructor, factories)
        } while (factories.size != previousCount)
        val roles = classifyColors() + factoryReturnSites.associateWith { ThemeColorRole.FACTORY }
        return ThemeInstructionMatches(constructor, components, gradientReturn, containerStores, roles)
    }

    private fun findAllocations(): Map<Int, Allocation> {
        val candidates = mutableMapOf<Int, Pair<Int, Allocation>>()
        for (position in codeStart until codeEnd - ALLOCATION_STUB_BYTES step INSTRUCTION_BYTES) {
            val first = word(position)
            val second = word(position + INSTRUCTION_BYTES)
            if (first and MOVE_IMMEDIATE_MASK != MOVE_ZERO || second and MOVE_IMMEDIATE_MASK != MOVE_KEEP) continue
            if (destinationRegister(first) != destinationRegister(second)) continue
            if (second ushr HALFWORD_SELECTOR_SHIFT and HALFWORD_SELECTOR_MASK != 1) continue
            if (word(position + ALLOCATION_BRANCH_OFFSET) and DIRECT_CALL_MASK != DIRECT_BRANCH) continue
            val header = (first ushr IMMEDIATE_HALFWORD_SHIFT and IMMEDIATE_HALFWORD_MASK) or
                ((second ushr IMMEDIATE_HALFWORD_SHIFT and IMMEDIATE_HALFWORD_MASK) shl HALFWORD_SHIFT)
            val allocation = Allocation(
                header ushr CLASS_ID_SHIFT and CLASS_ID_MASK,
                (header ushr ALLOCATION_SIZE_SHIFT and ALLOCATION_SIZE_MASK) * ALLOCATION_ALIGNMENT,
            )
            candidates[position] = branchTarget(position + ALLOCATION_BRANCH_OFFSET) to allocation
        }
        val allocator = candidates.values.groupingBy { it.first }.eachCount().maxByOrNull { it.value }?.key
            ?: throw PatchException("Could not find the Dart object allocator")
        return candidates.filterValues { it.first == allocator }.mapValues { it.value.second }
    }

    private fun findBarrierWrappers(): Set<Int> = (codeStart until codeEnd - BARRIER_WRAPPER_BYTES step INSTRUCTION_BYTES)
        .filter { position ->
            word(position) == BARRIER_WRAPPER.first() && BARRIER_WRAPPER.withIndex().all { (index, instruction) ->
                val actual = word(position + index * INSTRUCTION_BYTES)
                when (index) {
                    BARRIER_MOVE_INDEX -> actual and MOVE_REGISTER_MASK == MOVE_REGISTER && destinationRegister(actual) == RECEIVER_REGISTER
                    BARRIER_LOAD_INDEX -> actual and THREAD_LOAD_MASK == THREAD_LOAD_LINK
                    else -> actual == instruction
                }
            }
        }.toSet()

    private fun reachableFunctionInstructions(): Sequence<List<Int>> = sequence {
        for ((index, entry) in entries.withIndex()) {
            val end = entries.getOrElse(index + 1) { codeEnd }
            val visited = BooleanArray((end - entry) / INSTRUCTION_BYTES)
            val pending = ArrayDeque<Int>()
            val positions = mutableListOf<Int>()
            pending.addLast(entry)
            while (pending.isNotEmpty()) {
                val position = pending.removeLast()
                if (position !in entry until end || (position - entry) % INSTRUCTION_BYTES != 0) continue
                val slot = (position - entry) / INSTRUCTION_BYTES
                if (visited[slot]) continue
                visited[slot] = true
                positions += position
                val instruction = word(position)
                if (instruction == RETURN || instruction and INDIRECT_CALL_MASK == INDIRECT_BRANCH) continue
                val target = conditionalTarget(position, instruction)
                if (instruction and DIRECT_CALL_MASK == DIRECT_BRANCH) {
                    pending.addLast(branchTarget(position))
                } else {
                    target?.let(pending::addLast)
                    pending.addLast(position + INSTRUCTION_BYTES)
                }
            }
            yield(positions)
        }
    }

    private fun conditionalTarget(position: Int, instruction: Int): Int? = when {
        instruction and CONDITIONAL_BRANCH_MASK == CONDITIONAL_BRANCH || instruction and COMPARE_BRANCH_MASK == COMPARE_BRANCH ->
            position + (instruction shl CONDITIONAL_SIGN_SHIFT shr CONDITIONAL_SIGN_SHIFT shr CONDITIONAL_IMMEDIATE_SHIFT shl BRANCH_SCALE_SHIFT)
        instruction and COMPARE_BRANCH_MASK == TEST_BRANCH ->
            position + (instruction shl TEST_SIGN_SHIFT shr TEST_SIGN_SHIFT shr CONDITIONAL_IMMEDIATE_SHIFT shl BRANCH_SCALE_SHIFT)
        else -> null
    }

    private fun collectColors(constructor: Int, factories: MutableSet<Int>) {
        for (instructions in reachableFunctionInstructions()) {
            if (instructions.isEmpty()) continue
            val entry = instructions.first()
            val positions = instructions.toSet()
            val leaders = mutableSetOf(entry)
            val repeatRanges = mutableListOf<IntRange>()
            for (position in instructions) {
                val instruction = word(position)
                val target = if (instruction and DIRECT_CALL_MASK == DIRECT_BRANCH) branchTarget(position)
                    else conditionalTarget(position, instruction)
                if (target != null) {
                    leaders += target
                    if (target <= position && target in positions) repeatRanges += target..position
                }
                if (target != null || instruction == RETURN || instruction and INDIRECT_CALL_MASK == INDIRECT_BRANCH) {
                    leaders += position + INSTRUCTION_BYTES
                }
            }
            val blocks = linkedMapOf<Int, MutableList<Int>>()
            var block: MutableList<Int>? = null
            for (position in instructions.sorted()) {
                if (position in leaders || block == null || position != block.last() + INSTRUCTION_BYTES) {
                    block = mutableListOf()
                    blocks[position] = block
                }
                block.add(position)
            }
            val unknown = NativeObject(Allocation(UNREGISTERED_COLOR, 0), multipleInstances = true)
            val unknownValues = setOf(unknown)
            val objectsByCall = mutableMapOf<Int, NativeObject>()
            val incomingStates = mutableMapOf<Int, BranchState>()
            val pending = ArrayDeque<Int>()
            val queued = mutableSetOf<Int>()
            fun enqueue(target: Int) {
                if (queued.add(target)) pending.addLast(target)
            }
            fun <K> mergeValues(first: Map<K, Set<NativeObject>>, second: Map<K, Set<NativeObject>>) =
                (first.keys + second.keys).associateWith { (first[it] ?: unknownValues) + (second[it] ?: unknownValues) }
            fun offer(target: Int, state: BranchState) {
                if (target !in blocks) return
                val previous = incomingStates[target]
                val merged = if (previous == null) state else BranchState(
                    Array(REGISTER_COUNT) { previous.registers[it] + state.registers[it] },
                    (previous.locals.keys + state.locals.keys).associateWith { slot ->
                        val first = previous.locals[slot] ?: FrameValue(unknownValues)
                        val second = state.locals[slot] ?: FrameValue(unknownValues)
                        val offsets = first.frameOffsets + second.frameOffsets
                        FrameValue(first.objects + second.objects, if (offsets.size > 1) setOf(null) else offsets)
                    },
                    mergeValues(previous.fields, state.fields),
                    (previous.frameOffsets.keys + state.frameOffsets.keys).associateWith { register ->
                        when {
                            register !in previous.frameOffsets -> state.frameOffsets[register]
                            register !in state.frameOffsets -> previous.frameOffsets[register]
                            else -> previous.frameOffsets[register].takeIf { it == state.frameOffsets[register] }
                        }
                    },
                )
                if (previous != null && previous.registers.contentEquals(merged.registers) &&
                    previous.locals == merged.locals && previous.fields == merged.fields &&
                    previous.frameOffsets == merged.frameOffsets) return
                incomingStates[target] = merged
                enqueue(target)
            }
            offer(entry, BranchState(Array(REGISTER_COUNT) { unknownValues }, emptyMap(), emptyMap(), mapOf(FRAME_POINTER to 0)))
            while (pending.isNotEmpty()) {
                val start = pending.removeFirst()
                queued.remove(start)
                val incoming = incomingStates.getValue(start)
                val registers = incoming.registers.copyOf()
                val locals = incoming.locals.toMutableMap()
                val fields = incoming.fields.toMutableMap()
                val frameOffsets = incoming.frameOffsets.toMutableMap()
                var colorIdentityChanged = false
                val body = blocks.getValue(start)
                for (position in body) {
                    val instruction = word(position)
                    if (instruction and DIRECT_CALL_MASK == DIRECT_BRANCH || conditionalTarget(position, instruction) != null ||
                        instruction and INDIRECT_CALL_MASK == INDIRECT_BRANCH) continue
                    var loadedFrameOffsets: Set<Int?> = emptySet()
                    val destination = destinationRegister(instruction)
                    val source = sourceRegister(instruction)
                    val sourceIsFrameAddress = source in frameOffsets
                    val sourceFrameOffset = frameOffsets[source]
                    val createsFrameAddress = when {
                        instruction and MOVE_REGISTER_MASK == MOVE_REGISTER ->
                            instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK in frameOffsets
                        instruction and ADD_SUB_ADDRESS_MASK == ADD_SUB_IMMEDIATE_ADDRESS -> sourceIsFrameAddress
                        instruction and ADD_SUB_ADDRESS_MASK == ADD_SUB_REGISTER_ADDRESS ->
                            sourceIsFrameAddress || instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK in frameOffsets
                        else -> false
                    }
                    val createdFrameOffset = when {
                        instruction and MOVE_REGISTER_MASK == MOVE_REGISTER ->
                            frameOffsets[instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK]
                        instruction and ADD_SUB_ADDRESS_MASK == ADD_SUB_IMMEDIATE_ADDRESS -> {
                            val shift = if (instruction and ADDRESS_SHIFT_FLAG != 0) ADDRESS_SHIFT_BITS else 0
                            val immediate = (instruction ushr UNSIGNED_MEMORY_OFFSET_SHIFT and UNSIGNED_MEMORY_OFFSET_MASK) shl shift
                            sourceFrameOffset?.let { if (instruction and SUBTRACT_FLAG != 0) it - immediate else it + immediate }
                        }
                        else -> null
                    }
                    val statusRegister = arm64CompareAndSwapResultRegister(instruction) ?: arm64ExclusiveStoreStatusRegister(instruction)
                    val storesInteger = instruction and UNSCALED_MEMORY_MASK in UNSCALED_STORES ||
                        instruction and UNSIGNED_MEMORY_MASK in UNSIGNED_STORES
                    val storesSingle = instruction and MEMORY_LOAD_FLAG == 0 &&
                        instruction and SINGLE_MEMORY_KIND_MASK in SINGLE_MEMORY_KINDS &&
                        (instruction and SIMD_MEMORY_FLAG != 0 || instruction and SIMD_QUAD_FLAG == 0)
                    val storesPair = instruction and PAIR_STORE_MASK == PAIR_STORE
                    val pairSizeCode = instruction ushr MEMORY_PAIR_SIZE_SHIFT
                    val pairElementBytes = Int.SIZE_BYTES shl (if (instruction and SIMD_MEMORY_FLAG != 0) pairSizeCode else pairSizeCode ushr 1)
                    val storesExclusive = instruction and EXCLUSIVE_MEMORY_MASK == EXCLUSIVE_MEMORY &&
                        (instruction and MEMORY_LOAD_FLAG == 0 || arm64CompareAndSwapResultRegister(instruction) != null)
                    val storesAtomic = instruction and ATOMIC_UPDATE_MASK == ATOMIC_UPDATE
                    if (source != FRAME_POINTER && (storesSingle || storesPair || storesExclusive || storesAtomic)) {
                        val unknownAddress = unknown in registers[source]
                        if (!storesInteger || unknownAddress) fields.clear()
                    }
                    if (sourceIsFrameAddress && (storesExclusive || storesAtomic)) locals.clear()
                    if (sourceIsFrameAddress && storesSingle) {
                        val width = instruction ushr MEMORY_WIDTH_SHIFT
                        val size = 1 shl (if (instruction and SIMD_MEMORY_FLAG != 0 && instruction and SIMD_QUAD_FLAG != 0)
                            QUAD_MEMORY_WIDTH_SHIFT else width)
                        val unsigned = instruction and SINGLE_MEMORY_KIND_MASK == UNSIGNED_MEMORY_KIND
                        if (sourceFrameOffset == null || !unsigned && instruction and REGISTER_OFFSET_FLAG != 0) {
                            locals.clear()
                        } else {
                            val postIndexed = instruction and SINGLE_INDEX_MODE_MASK == SINGLE_POST_INDEX
                            val offset = sourceFrameOffset + if (unsigned)
                                (instruction ushr UNSIGNED_MEMORY_OFFSET_SHIFT and UNSIGNED_MEMORY_OFFSET_MASK) * size
                                else if (postIndexed) 0 else signedMemoryOffset(instruction)
                            locals.keys.removeAll { it.offset < offset + size && it.offset + it.byteCount > offset }
                        }
                    }
                    when {
                        instruction == RETURN -> {
                            for (value in registers[RETURN_REGISTER]) {
                                val callers = value.colorReturnAddresses.filter { it >= 0 }
                                if (callers.isNotEmpty()) {
                                    factories += entry
                                    factoryReturnSites += callers
                                }
                            }
                        }
                        instruction and MOVE_REGISTER_MASK == MOVE_REGISTER ->
                            registers[destination] = registers[instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK]
                        instruction and DECOMPRESS_POINTER_MASK == DECOMPRESS_POINTER &&
                            instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK == HEAP_REGISTER ->
                            registers[destination] = registers[source]
                        instruction and DIRECT_CALL_MASK == DIRECT_CALL || instruction and INDIRECT_CALL_MASK == INDIRECT_CALL -> {
                            val target = if (instruction and DIRECT_CALL_MASK == DIRECT_CALL) branchTarget(position) else -1
                            if (target == constructor) {
                                for (value in registers[RECEIVER_REGISTER]) {
                                    if (value !== unknown && value.colorReturnAddresses.add(position + INSTRUCTION_BYTES)) colorIdentityChanged = true
                                }
                            }
                            val incomingFrameArgument = frameOffsets.keys.any { it != FRAME_POINTER && it != DART_STACK_POINTER }
                            if (target in barrierWrappers) {
                                for (register in BARRIER_SCRATCH_REGISTERS) {
                                    registers[register] = unknownValues
                                    frameOffsets.remove(register)
                                }
                            } else if (target in allocations) {
                                for (register in 0 until ALLOCATION_SCRATCH_REGISTERS) {
                                    registers[register] = unknownValues
                                    if (register != DART_STACK_POINTER) frameOffsets.remove(register)
                                }
                            } else {
                                registers.fill(unknownValues)
                                frameOffsets.keys.retainAll(setOf(FRAME_POINTER, DART_STACK_POINTER))
                                frameOffsets[FRAME_POINTER] = 0
                                if (target != constructor) {
                                    fields.clear()
                                    if (incomingFrameArgument || locals.values.any { it.frameOffsets.isNotEmpty() }) locals.clear()
                                }
                            }
                            allocations[target]?.let { allocation ->
                                registers[RETURN_REGISTER] = setOf(objectsByCall.getOrPut(position) { NativeObject(allocation, multipleInstances = repeatRanges.any { position in it }) })
                            }
                            if (target in factories) {
                                registers[RETURN_REGISTER] = setOf(objectsByCall.getOrPut(position) {
                                    NativeObject(Allocation(UNREGISTERED_COLOR, 0), position + INSTRUCTION_BYTES, repeatRanges.any { position in it })
                                })
                            }
                        }
                        instruction and UNSCALED_MEMORY_MASK in UNSCALED_LOADS ||
                            instruction and UNSIGNED_MEMORY_MASK in UNSIGNED_LOADS -> {
                            val offset = if (instruction and UNSCALED_MEMORY_MASK in UNSCALED_LOADS)
                                signedMemoryOffset(instruction) else unsignedMemoryOffset(instruction)
                            val byteCount = 1 shl (instruction ushr MEMORY_WIDTH_SHIFT)
                            registers[destination] = if (source == FRAME_POINTER) {
                                val values = listOfNotNull(locals[FrameSlot(offset, byteCount)],
                                    if (byteCount == Int.SIZE_BYTES) locals[FrameSlot(offset, Long.SIZE_BYTES)] else null)
                                loadedFrameOffsets = values.flatMap { it.frameOffsets }.toSet()
                                values.flatMap { it.objects }.toSet().ifEmpty { unknownValues }
                            } else if (fields.isEmpty()) unknownValues
                                else registers[source].flatMap { receiver ->
                                    val values = fields[ObjectField(receiver, offset, byteCount)].orEmpty()
                                    val wideFieldValues = if (byteCount == Int.SIZE_BYTES)
                                        fields[ObjectField(receiver, offset, Long.SIZE_BYTES)].orEmpty() else emptySet()
                                    (values + wideFieldValues).ifEmpty { unknownValues }
                                }.toSet()
                        }
                        storesInteger -> {
                            val offset = if (instruction and UNSCALED_MEMORY_MASK in UNSCALED_STORES)
                                signedMemoryOffset(instruction) else unsignedMemoryOffset(instruction)
                            val values = registers[destination]
                            if (source == FRAME_POINTER) {
                                locals[FrameSlot(offset, 1 shl (instruction ushr MEMORY_WIDTH_SHIFT))] = FrameValue(values,
                                    if (destination in frameOffsets) setOf(frameOffsets[destination]) else emptySet())
                            } else {
                                for (receiver in registers[source]) {
                                    if (receiver === unknown) continue
                                    val byteCount = 1 shl (instruction ushr MEMORY_WIDTH_SHIFT)
                                    val field = ObjectField(receiver, offset, byteCount)
                                    if (registers[source].size == 1 && !receiver.multipleInstances) {
                                        fields.keys.removeAll { it.receiver === receiver &&
                                            it.offset < offset + byteCount && it.offset + it.byteCount > offset }
                                        fields[field] = values
                                    } else {
                                        fields[field] = (fields[field] ?: unknownValues) + values
                                    }
                                    if (instruction and UNSCALED_MEMORY_MASK in UNSCALED_STORES) {
                                        for (value in values) {
                                            for (caller in value.colorReturnAddresses) stores += ColorStore(caller, receiver.allocation, offset)
                                        }
                                    }
                                }
                            }
                        }
                        storesPair && sourceIsFrameAddress -> {
                            if (sourceFrameOffset == null) {
                                locals.clear()
                            } else {
                                val postIndexed = instruction and PAIR_INDEX_MODE_MASK == PAIR_POST_INDEX
                                val offset = sourceFrameOffset + if (postIndexed) 0
                                    else (instruction shl PAIR_OFFSET_SIGN_SHIFT shr PAIR_OFFSET_VALUE_SHIFT) * pairElementBytes
                                locals.keys.removeAll { it.offset < offset + pairElementBytes * 2 && it.offset + it.byteCount > offset }
                            }
                        }
                        instruction and UNSCALED_MEMORY_MASK in FLOAT_STORES -> {
                            for (value in registers[source]) floatingFields.getOrPut(value.allocation.classId, ::mutableSetOf) += signedMemoryOffset(instruction)
                        }
                        statusRegister != null -> registers[statusRegister] = unknownValues
                        writesArm64GeneralRegister(instruction) -> registers[destination] = unknownValues
                    }
                    val writtenRegister = statusRegister ?: destination.takeIf { writesArm64GeneralRegister(instruction) }
                    if (writtenRegister != null) {
                        frameOffsets.remove(writtenRegister)
                        if (writtenRegister == FRAME_POINTER) {
                            locals.clear()
                            for (register in frameOffsets.keys.toList()) frameOffsets[register] = null
                        }
                    }
                    if (createsFrameAddress) frameOffsets[destination] = createdFrameOffset
                    if (loadedFrameOffsets.isNotEmpty()) frameOffsets[destination] = loadedFrameOffsets.singleOrNull()
                    if (destination == FRAME_POINTER && instruction and MOVE_REGISTER_MASK == MOVE_REGISTER &&
                        instruction ushr THIRD_REGISTER_SHIFT and REGISTER_MASK == DART_STACK_POINTER) {
                        frameOffsets[DART_STACK_POINTER] = 0
                    }
                    arm64SecondLoadRegister(instruction)?.let {
                        registers[it] = unknownValues
                        frameOffsets.remove(it)
                    }
                    if (instruction and SINGLE_WRITEBACK_MASK == SINGLE_WRITEBACK ||
                        instruction and PAIR_WRITEBACK_MASK == PAIR_WRITEBACK) {
                        registers[source] = unknownValues
                        if (sourceIsFrameAddress) {
                            val delta = if (instruction and SINGLE_WRITEBACK_MASK == SINGLE_WRITEBACK) signedMemoryOffset(instruction)
                                else {
                                    (instruction shl PAIR_OFFSET_SIGN_SHIFT shr PAIR_OFFSET_VALUE_SHIFT) * pairElementBytes
                                }
                            frameOffsets[source] = sourceFrameOffset?.plus(delta)
                        }
                        if (source == FRAME_POINTER) locals.clear()
                    }
                    registers[ZERO_REGISTER] = unknownValues
                    frameOffsets.remove(ZERO_REGISTER)
                    frameOffsets[FRAME_POINTER] = 0
                }
                val last = body.last()
                val instruction = word(last)
                val state = BranchState(registers, locals, fields, frameOffsets)
                if (instruction and DIRECT_CALL_MASK == DIRECT_BRANCH) {
                    offer(branchTarget(last), state)
                } else if (instruction != RETURN && instruction and INDIRECT_CALL_MASK != INDIRECT_BRANCH) {
                    conditionalTarget(last, instruction)?.let { offer(it, state) }
                    offer(last + INSTRUCTION_BYTES, state)
                }
                if (colorIdentityChanged) incomingStates.keys.forEach(::enqueue)
            }
        }
    }

    private fun classifyColors(): Map<Int, ThemeColorRole> {
        fun type(size: Int, field: Int, minimum: Int, name: String) = stores
            .filter { it.caller >= 0 && it.allocation.size == size && it.field == field }
            .groupingBy { it.allocation.classId }.eachCount().filterValues { it >= minimum }.keys.toList().singleMatch(name)

        val text = type(TEXT_STYLE_SIZE, TEXT_COLOR_OFFSET, MIN_TEXT_COLOR_SITES, "text color field")
        val decoration = type(DECORATION_SIZE, BACKGROUND_COLOR_OFFSET, MIN_BACKGROUND_COLOR_SITES, "decoration color field")
        val icon = type(ICON_SIZE, ICON_COLOR_OFFSET, MIN_ICON_COLOR_SITES, "icon color field")
        val divider = type(SMALL_WIDGET_SIZE, DIVIDER_COLOR_OFFSET, MIN_DIVIDER_COLOR_SITES, "divider color field")
        val scaffold = type(SCAFFOLD_SIZE, TEXT_COLOR_OFFSET, MIN_SCAFFOLD_SITES, "scaffold background field")
        val appBar = type(APP_BAR_SIZE, APP_BAR_COLOR_OFFSET, MIN_APP_BAR_SITES, "app bar background field")
        val dialog = type(DIALOG_THEME_SIZE, DIALOG_COLOR_OFFSET, MIN_DIALOG_THEME_SITES, "dialog background field")
        val navigation = stores.filter { it.caller >= 0 }.groupBy { it.allocation.classId }.filterValues { fields ->
            fields.first().allocation.size == SMALL_WIDGET_SIZE &&
                fields.count { it.field == NAVIGATION_COLOR_OFFSET } >= MIN_NAVIGATION_SITES &&
                fields.count { it.field == NAVIGATION_SELECTED_COLOR_OFFSET } >= MIN_NAVIGATION_SITES
        }.keys.toList().singleMatch("navigation theme color fields")
        return stores.mapNotNull { store ->
            val role = when {
                store.allocation.classId == text && store.field == TEXT_COLOR_OFFSET -> ThemeColorRole.INK
                store.allocation.classId == decoration && store.field == BACKGROUND_COLOR_OFFSET -> ThemeColorRole.SURFACE
                store.allocation.classId == icon && store.field == ICON_COLOR_OFFSET -> ThemeColorRole.INK
                store.allocation.classId == divider && store.field == DIVIDER_COLOR_OFFSET -> ThemeColorRole.LINE
                store.allocation.classId == scaffold && store.field == TEXT_COLOR_OFFSET -> ThemeColorRole.PAGE
                store.allocation.classId == appBar && store.field == APP_BAR_COLOR_OFFSET -> ThemeColorRole.SURFACE
                store.allocation.classId == dialog && store.field == DIALOG_COLOR_OFFSET -> ThemeColorRole.SURFACE
                store.allocation.classId == navigation && store.field == NAVIGATION_COLOR_OFFSET -> ThemeColorRole.SURFACE
                store.allocation.classId == navigation && store.field == NAVIGATION_SELECTED_COLOR_OFFSET -> ThemeColorRole.INK
                store.allocation.size == BORDER_SIZE && store.field == BACKGROUND_COLOR_OFFSET ->
                    if (BORDER_WIDTH_OFFSET in floatingFields[store.allocation.classId].orEmpty()) ThemeColorRole.LINE
                    else ThemeColorRole.INK
                store.field == BACKGROUND_COLOR_OFFSET && SHADOW_BLUR_OFFSET in floatingFields[store.allocation.classId].orEmpty() ->
                    ThemeColorRole.PRESERVE
                else -> null
            }
            role?.let { store.caller to it }
        }.groupBy({ it.first }, { it.second }).mapNotNull { (caller, roles) ->
            roles.distinct().singleOrNull()?.let { caller to it }
        }.toMap()
    }

    private fun firstReturn(entry: Int): Int? {
        var position = entry + INSTRUCTION_BYTES
        while (position < codeEnd && word(position) != FRAME_ENTRY) {
            if (word(position) == RETURN) return position
            position += INSTRUCTION_BYTES
        }
        return null
    }

    private fun branchTarget(position: Int): Int = position + ((word(position) shl BRANCH_IMMEDIATE_SHIFT) shr BRANCH_IMMEDIATE_SHIFT shl BRANCH_SCALE_SHIFT)

    private fun word(offset: Int) = bytes.readInt(offset)

    private fun destinationRegister(instruction: Int) = instruction and REGISTER_MASK

    private fun sourceRegister(instruction: Int) = instruction ushr SECOND_REGISTER_SHIFT and REGISTER_MASK

    private fun signedMemoryOffset(instruction: Int) = instruction shl MEMORY_SIGN_EXTENSION_SHIFT shr MEMORY_SIGN_EXTENSION_SHIFT shr MEMORY_OFFSET_SHIFT

    private fun unsignedMemoryOffset(instruction: Int) =
        (instruction ushr UNSIGNED_MEMORY_OFFSET_SHIFT and UNSIGNED_MEMORY_OFFSET_MASK) shl (instruction ushr MEMORY_WIDTH_SHIFT)


}

private const val FRAME_LEAVE_BYTES = 12
private const val ALLOCATION_STUB_BYTES = 12
private const val ALLOCATION_BRANCH_OFFSET = 8
private const val HALFWORD_SELECTOR_MASK = 3
private const val MEMORY_SIGN_EXTENSION_SHIFT = 11
private const val MEMORY_OFFSET_SHIFT = 12
private const val UNSIGNED_MEMORY_MASK = 0xffc00000.toInt()
private const val UNSIGNED_MEMORY_OFFSET_SHIFT = 10
private const val UNSIGNED_MEMORY_OFFSET_MASK = 0xfff
private const val MEMORY_WIDTH_SHIFT = 30
private const val SINGLE_WRITEBACK_MASK = 0x3b200400
private const val SINGLE_WRITEBACK = 0x38000400
private const val PAIR_WRITEBACK_MASK = 0x3a800000
private const val PAIR_WRITEBACK = 0x28800000
private val UNSCALED_LOADS = setOf(0xf8400000.toInt(), 0xb8400000.toInt())
private val UNSCALED_STORES = setOf(0xf8000000.toInt(), 0xb8000000.toInt())
private val FLOAT_STORES = setOf(0xfc000000.toInt(), STORE_SINGLE)
private val UNSIGNED_LOADS = setOf(0xf9400000.toInt(), 0xb9400000.toInt())
private val UNSIGNED_STORES = setOf(0xf9000000.toInt(), 0xb9000000.toInt())

private fun List<Int>.singleMatch(name: String): Int = singleOrNull()
    ?: throw PatchException("Expected one $name, found $size")

private const val INDIRECT_BRANCH = 0xd61f0000.toInt()
private const val BARRIER_WRAPPER_BYTES = 32
private const val BARRIER_MOVE_INDEX = 2
private const val BARRIER_LOAD_INDEX = 3
private val BARRIER_SCRATCH_REGISTERS = setOf(16, 17)
private const val THREAD_LOAD_MASK = 0xffc003ff.toInt()
private const val THREAD_LOAD_LINK = 0xf940035e.toInt()
private val BARRIER_WRAPPER = listOf(0xf81f8dfe.toInt(), 0xf81f8de1.toInt(), 0, 0,
    0xd63f03c0.toInt(), 0xf84085e1.toInt(), 0xf84085fe.toInt(), RETURN)
