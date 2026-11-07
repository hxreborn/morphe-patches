/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.misc.arm64.arm64CompareAndSwapResultRegister
import app.morphe.patches.shared.misc.arm64.arm64ExclusiveStoreStatusRegister
import app.morphe.patches.shared.misc.arm64.arm64SecondLoadRegister
import app.morphe.patches.shared.misc.arm64.writesArm64GeneralRegister
import app.morphe.patches.shared.misc.dart.DartSnapshot
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.readInt
import kotlin.math.abs

private const val INSTRUCTION_BYTES = 4
private const val REGISTER_MASK = 31
private const val POOL_REGISTER = 27
private const val BASE_REGISTER_SHIFT = 5
private const val LOAD_IMMEDIATE_SHIFT = 10
private const val LOAD_IMMEDIATE_MASK = 4095
private const val POOL_BASE_WINDOW = 12
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_OPCODE_MASK = 0xff000000.toInt()
private const val ADD_SHIFT_FLAG = 1 shl 22
private const val ADD_IMMEDIATE_SHIFT = 10
private const val ADD_IMMEDIATE_MASK = 4095
private const val LOAD_UNSIGNED = 0xf9400000.toInt()
private const val LOAD_OPCODE_MASK = 0xffc00000.toInt()
private const val BRANCH_WITH_LINK = 0x94000000.toInt()
private const val BRANCH_MASK = 0xfc000000.toInt()
private const val BRANCH = 0x14000000
private const val CONDITIONAL_BRANCH_MASK = 0xff000010.toInt()
private const val CONDITIONAL_BRANCH = 0x54000000
private const val COMPARE_BRANCH_MASK = 0x7e000000
private const val COMPARE_BRANCH = 0x34000000
private const val TEST_BRANCH = 0x36000000
private const val BRANCH_IMMEDIATE_BITS = 26
private const val CONDITIONAL_IMMEDIATE_SHIFT = 5
private const val CONDITIONAL_IMMEDIATE_BITS = 19
private const val TEST_IMMEDIATE_BITS = 14
private const val BRANCH_REGISTER = 0xd61f0000.toInt()
private const val RETURN_REGISTER = 0xd65f0000.toInt()
private val TERMINATORS = setOf(BRANCH_REGISTER, RETURN_REGISTER)
private const val BRANCH_REGISTER_WITH_LINK = 0xd63f0000.toInt()
private const val BRANCH_REGISTER_MASK = 0xfffffc1f.toInt()
private const val SINGLE_WRITEBACK_MASK = 0x3b200400
private const val SINGLE_WRITEBACK = 0x38000400
private const val PAIR_WRITEBACK_MASK = 0x3a800000
private const val PAIR_WRITEBACK = 0x28800000
private const val FRAME_PROLOGUE = 0xa9bf79fd.toInt()
private const val FRAME_POINTER = 0xaa0f03fd.toInt()

internal lateinit var nativeMethods: NativeMethods
    private set

internal val nativeMethodsPatch = resourcePatch {
    execute { nativeMethods = NativeMethods(dartAotLibrary().readBytes()) }
}

internal class NativeFunction(val start: Int, val end: Int, val stringLoads: Map<Int, String>) {
    fun loads(value: String): List<Int> = stringLoads.filterValues { it == value }.keys.toList()
}

internal class NativeMethods(val bytes: ByteArray) {
    val snapshot = DartSnapshot(bytes)
    private val codeStart = snapshot.executableOffsets.first
    private val codeEnd = snapshot.executableOffsets.last + 1
    private val instructionReachability = reachableInstructions()
    private val calledEntries = mutableSetOf<Int>()
    private val functions: List<NativeFunction>
    private data class PoolLoad(val offset: Int, val starts: Set<Int>)

    private val poolLoads = mutableMapOf<Int, PoolLoad>()

    init {
        val pool = snapshot.namesByPoolOffset
        val starts = mutableListOf<Int>()
        val loads = mutableMapOf<Int, MutableMap<Int, String>>()
        val incomingBases = poolBases()
        var function = -1
        for (address in codeStart until codeEnd step INSTRUCTION_BYTES) {
            val instruction = bytes.readInt(address)
            if (isFunctionEntry(address) || address in calledEntries) {
                function = address
                starts += address
            }
            val bases = incomingBases[(address - codeStart) / INSTRUCTION_BYTES] ?: continue
            if (instruction and LOAD_OPCODE_MASK != LOAD_UNSIGNED) continue
            val baseRegister = instruction ushr BASE_REGISTER_SHIFT and REGISTER_MASK
            val base = bases[baseRegister]?.takeIf { it.addresses.all { origin -> abs(address - origin) <= POOL_BASE_WINDOW } }
            val offset = (instruction ushr LOAD_IMMEDIATE_SHIFT and LOAD_IMMEDIATE_MASK) * Long.SIZE_BYTES
            val poolOffset = when {
                baseRegister == POOL_REGISTER -> offset
                base != null -> base.offset + offset
                else -> continue
            }
            poolLoads[address] = PoolLoad(poolOffset, if (baseRegister == POOL_REGISTER) setOf(address) else base!!.addresses)
            if (function >= 0) pool[poolOffset]?.let { loads.getOrPut(function) { mutableMapOf() }[address] = it }
        }
        functions = starts.mapIndexed { index, start ->
            NativeFunction(start, starts.getOrElse(index + 1) { codeEnd }, loads[start].orEmpty())
        }
    }

    private data class PoolBase(val offset: Int, val addresses: Set<Int>)

    private fun isFunctionEntry(address: Int): Boolean = address + INSTRUCTION_BYTES < codeEnd &&
        bytes.readInt(address) == FRAME_PROLOGUE && bytes.readInt(address + INSTRUCTION_BYTES) == FRAME_POINTER

    private fun poolBases(): Array<Map<Int, PoolBase>?> {
        val incoming = arrayOfNulls<Map<Int, PoolBase>>(instructionReachability.size)
        val queued = BooleanArray(incoming.size)
        val pending = ArrayDeque<Int>()
        fun offer(address: Int, bases: Map<Int, PoolBase>) {
            if (address !in codeStart until codeEnd || (address - codeStart) % INSTRUCTION_BYTES != 0) return
            val index = (address - codeStart) / INSTRUCTION_BYTES
            val previous = incoming[index]
            val merged = if (previous == null) bases else previous.mapNotNull { (register, value) ->
                val other = bases[register]?.takeIf { it.offset == value.offset } ?: return@mapNotNull null
                register to PoolBase(value.offset, value.addresses + other.addresses)
            }.toMap()
            if (previous == merged) return
            incoming[index] = merged
            if (!queued[index]) {
                queued[index] = true
                pending.addLast(address)
            }
        }
        offer(codeStart, emptyMap())
        for (address in codeStart until codeEnd step INSTRUCTION_BYTES) {
            if (isFunctionEntry(address)) offer(address, emptyMap())
        }
        while (pending.isNotEmpty()) {
            val address = pending.removeFirst()
            val index = (address - codeStart) / INSTRUCTION_BYTES
            queued[index] = false
            val instruction = bytes.readInt(address)
            val bases = transferPoolBases(address, instruction, incoming[index]!!)
            if (instruction and BRANCH_MASK == BRANCH_WITH_LINK) {
                val target = callTarget(address)
                if (target in codeStart until codeEnd && (target - codeStart) % INSTRUCTION_BYTES == 0) {
                    calledEntries += target
                    offer(target, emptyMap())
                }
            }
            branchTarget(address, instruction)?.let { offer(it, bases) }
            if (instruction and BRANCH_MASK != BRANCH && instruction and BRANCH_REGISTER_MASK !in TERMINATORS) {
                offer(address + INSTRUCTION_BYTES, bases)
            }
        }
        return incoming
    }

    private fun transferPoolBases(address: Int, instruction: Int, bases: Map<Int, PoolBase>): Map<Int, PoolBase> {
        if (isFunctionEntry(address) || instruction and BRANCH_MASK == BRANCH_WITH_LINK ||
            instruction and BRANCH_REGISTER_MASK == BRANCH_REGISTER_WITH_LINK) return emptyMap()
        val register = instruction and REGISTER_MASK
        val base = instruction ushr BASE_REGISTER_SHIFT and REGISTER_MASK
        if (instruction and ADD_OPCODE_MASK == ADD_IMMEDIATE && base == POOL_REGISTER) {
            val immediate = instruction ushr ADD_IMMEDIATE_SHIFT and ADD_IMMEDIATE_MASK
            val offset = immediate shl (if (instruction and ADD_SHIFT_FLAG != 0) CLASS_PAGE_SHIFT else 0)
            return if (register == REGISTER_MASK) bases else bases + (register to PoolBase(offset, setOf(address)))
        }
        val status = arm64CompareAndSwapResultRegister(instruction) ?: arm64ExclusiveStoreStatusRegister(instruction)
        val destination = status ?: register.takeIf { writesArm64GeneralRegister(instruction) }
        val second = arm64SecondLoadRegister(instruction)
        val writeback = base.takeIf {
            instruction and SINGLE_WRITEBACK_MASK == SINGLE_WRITEBACK || instruction and PAIR_WRITEBACK_MASK == PAIR_WRITEBACK
        }
        if (destination !in bases && second !in bases && writeback !in bases) return bases
        return bases.filterKeys { it != destination && it != second && it != writeback }
    }

    private fun reachableInstructions(): BooleanArray {
        val start = snapshot.executableOffsets.first
        val end = snapshot.executableOffsets.last + 1
        val reachable = BooleanArray((end - start) / INSTRUCTION_BYTES)
        val pending = mutableListOf(start)
        for (address in start until end - INSTRUCTION_BYTES step INSTRUCTION_BYTES) {
            if (bytes.readInt(address) == FRAME_PROLOGUE && bytes.readInt(address + INSTRUCTION_BYTES) == FRAME_POINTER) {
                pending += address
            }
        }
        while (pending.isNotEmpty()) {
            var address = pending.removeAt(pending.lastIndex)
            while (address in start until end && (address - start) % INSTRUCTION_BYTES == 0) {
                val index = (address - start) / INSTRUCTION_BYTES
                if (reachable[index]) break
                reachable[index] = true
                val instruction = bytes.readInt(address)
                if (instruction and BRANCH_MASK == BRANCH_WITH_LINK) pending += callTarget(address)
                val target = branchTarget(address, instruction)
                if (target != null) {
                    pending += target
                    if (instruction and BRANCH_MASK == BRANCH) break
                }
                if (instruction and BRANCH_REGISTER_MASK in TERMINATORS) break
                address += INSTRUCTION_BYTES
            }
        }
        return reachable
    }

    private fun branchTarget(address: Int, instruction: Int): Int? {
        val (shift, bits) = when {
            instruction and BRANCH_MASK == BRANCH -> 0 to BRANCH_IMMEDIATE_BITS
            instruction and CONDITIONAL_BRANCH_MASK == CONDITIONAL_BRANCH || instruction and COMPARE_BRANCH_MASK == COMPARE_BRANCH ->
                CONDITIONAL_IMMEDIATE_SHIFT to CONDITIONAL_IMMEDIATE_BITS
            instruction and COMPARE_BRANCH_MASK == TEST_BRANCH -> CONDITIONAL_IMMEDIATE_SHIFT to TEST_IMMEDIATE_BITS
            else -> return null
        }
        val signShift = Int.SIZE_BITS - bits
        return address + (instruction ushr shift shl signShift shr signShift) * INSTRUCTION_BYTES
    }

    fun all(): List<NativeFunction> = functions

    fun containing(address: Int): NativeFunction = functions.single { address >= it.start && address < it.end }

    fun poolOffset(address: Int): Int = poolLoads[address]?.offset
        ?: throw PatchException("Expected Dart object pool load")

    fun poolLoads(function: NativeFunction): List<Int> = poolLoads.keys.filter { it >= function.start && it < function.end }

    fun references(poolOffset: Int): List<Int> = poolLoads.filterValues { it.offset == poolOffset }.keys.toList()

    fun asyncBodyStart(function: NativeFunction): Int {
        val trigger = single("/api/base/v1/review/trigger")
        val candidates = firstCalls(trigger.start, trigger.end, trigger.start)
        val invocation = candidates.singleOrNull { candidate ->
            firstCalls(candidate + INSTRUCTION_BYTES, trigger.end, trigger.start).none { it != candidate && it in candidates }
        } ?: throw PatchException("Expected one Dart async initializer after rejoining calls")
        val initializer = callTarget(invocation)
        return calls(function).single { callTarget(it) == initializer } + INSTRUCTION_BYTES
    }

    private fun isReachable(address: Int): Boolean = address in codeStart until codeEnd &&
        instructionReachability[(address - codeStart) / INSTRUCTION_BYTES]

    fun calls(function: NativeFunction): List<Int> = (function.start until function.end step INSTRUCTION_BYTES).filter {
        isReachable(it) && bytes.readInt(it) and BRANCH_MASK == BRANCH_WITH_LINK
    }

    fun matching(vararg strings: String): List<NativeFunction> = functions.filter { function ->
        strings.all { it in function.stringLoads.values }
    }

    fun single(vararg strings: String, predicate: (NativeFunction) -> Boolean = { true }): NativeFunction =
        matching(*strings).singleOrNull(predicate)
        ?: throw PatchException("Expected one Dart function containing ${strings.joinToString()}")

    fun poolLoadStart(address: Int): Int = poolLoads[address]?.starts?.singleOrNull()
        ?: throw PatchException("Expected Dart object pool load with one defining instruction")

    fun poolLoad(address: Int, register: Int): List<Int> {
        val offset = poolLoads[address]?.offset ?: throw PatchException("Expected Dart object pool load")
        return if (offset < POOL_DIRECT_LIMIT) {
            listOf(LOAD_UNSIGNED or ((offset / Long.SIZE_BYTES) shl LOAD_IMMEDIATE_SHIFT) or
                (POOL_REGISTER shl BASE_REGISTER_SHIFT) or register)
        } else {
            listOf(
                ADD_SHIFTED or ((offset ushr CLASS_PAGE_SHIFT) shl ADD_IMMEDIATE_SHIFT) or
                    (POOL_REGISTER shl BASE_REGISTER_SHIFT) or register,
                LOAD_UNSIGNED or (((offset and ADD_IMMEDIATE_MASK) / Long.SIZE_BYTES) shl LOAD_IMMEDIATE_SHIFT) or
                    (register shl BASE_REGISTER_SHIFT) or register,
            )
        }
    }

    fun firstCallAfter(address: Int, end: Int): Int = firstCalls(address + INSTRUCTION_BYTES, end, containing(address).start).let { calls ->
        calls.singleOrNull() ?: throw PatchException("Expected one first Dart invocation, got ${calls.size}")
    }

    private fun firstCalls(address: Int, end: Int, functionStart: Int): Set<Int> {
        val reachable = mutableSetOf<Int>()
        val pending = ArrayDeque<Int>()
        val firstCalls = mutableSetOf<Int>()
        pending.addLast(address)
        while (pending.isNotEmpty()) {
            val position = pending.removeLast()
            if (position !in functionStart until minOf(end, codeEnd) || (position - codeStart) % INSTRUCTION_BYTES != 0 ||
                !reachable.add(position)) continue
            val instruction = bytes.readInt(position)
            if (instruction and BRANCH_MASK == BRANCH_WITH_LINK ||
                instruction and BRANCH_REGISTER_MASK == BRANCH_REGISTER_WITH_LINK) {
                firstCalls += position
                continue
            }
            branchTarget(position, instruction)?.let(pending::addLast)
            if (instruction and BRANCH_MASK != BRANCH && instruction and BRANCH_REGISTER_MASK !in TERMINATORS) {
                pending.addLast(position + INSTRUCTION_BYTES)
            }
        }
        return firstCalls
    }

    fun callTarget(address: Int): Int {
        val instruction = bytes.readInt(address)
        if (instruction and BRANCH_MASK != BRANCH_WITH_LINK) throw PatchException("Expected Dart direct call")
        return address + ((instruction shl BRANCH_SIGN_SHIFT) shr BRANCH_SCALE_SHIFT)
    }

    private companion object {
        const val POOL_DIRECT_LIMIT = 32768
        val ADD_SHIFTED = 0x91400000.toInt()
        const val BRANCH_SIGN_SHIFT = 6
        const val BRANCH_SCALE_SHIFT = 4
        const val CLASS_PAGE_SHIFT = 12
    }
}
