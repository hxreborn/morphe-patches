/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt

private const val INSTRUCTION_BYTES = 4
private const val FRAME_PROLOGUE = 0xa9bf79fd.toInt()
private const val FRAME_POINTER = 0xaa0f03fd.toInt()
private const val REGISTER_MASK = 31
private const val BASE_REGISTER_SHIFT = 5
private const val POOL_REGISTER = 27
private const val LOAD_MASK = 0xffc00000.toInt()
private const val LOAD_UNSIGNED = 0xf9400000.toInt()
private const val IMMEDIATE_SHIFT = 10
private const val IMMEDIATE_MASK = 4095
private const val ADD_MASK = 0xff000000.toInt()
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_SHIFT_FLAG = 1 shl 22
private const val PAGE_SHIFT = 12
private const val CALL_MASK = 0xfc000000.toInt()
private const val CALL = 0x94000000.toInt()
private const val CALL_SIGN_SHIFT = 6
private const val PREMIUM_BRANCH_BEFORE_IMPORT_CALL = 3 * INSTRUCTION_BYTES
private const val BOOLEAN_BRANCH_MASK = 0xfff8001f.toInt()
private const val BRANCH_ON_PREMIUM = 0x36200000
private const val MOVE_FREE_IMPORT_ALLOWANCE = 0xd2800061.toInt()
private const val NOP = 0xd503201f.toInt()
private const val COUNTER_KEY = "singleFlightFreeImportCount"
private const val PREMIUM_KEY = "isPremiumEnabled"
private val IMPORTER_STRINGS = setOf("calendar_single_import_completed", "is_free", "remaining")
private val RETURN_MAXIMUM_IMPORT_COUNT = intArrayOf(0xd29fffe0.toInt(), 0xf2afffe0.toInt(), 0xd65f03c0.toInt())

internal class CalendarImport(private val bytes: ByteArray, private val code: IntRange, stringsByPoolOffset: Map<Int, String>) {
    private val instructions = code.step(INSTRUCTION_BYTES)
    private val entries = instructions.filter { address ->
        address + INSTRUCTION_BYTES <= code.last && bytes.readInt(address) == FRAME_PROLOGUE &&
            bytes.readInt(address + INSTRUCTION_BYTES) == FRAME_POINTER
    }
    private val stringsByFunction = run {
        val anchors = IMPORTER_STRINGS + setOf(COUNTER_KEY, PREMIUM_KEY)
        val pool = stringsByPoolOffset.filterValues { it in anchors }
        instructions.mapNotNull { address ->
            val value = pool[poolOffset(address)] ?: return@mapNotNull null
            functionAt(address) to value
        }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.toSet() }
    }

    fun removeLimit() {
        val importer = stringsByFunction.filterValues { it.containsAll(IMPORTER_STRINGS) }.keys.singleOrNull()
            ?: throw PatchException("Expected one per-flight calendar importer")
        val premiumBranch = findPremiumBranch(importer)
        val remainingCount = findRemainingCount(importer)

        bytes.writeInt(premiumBranch, NOP)
        RETURN_MAXIMUM_IMPORT_COUNT.forEachIndexed { index, instruction ->
            bytes.writeInt(remainingCount + index * INSTRUCTION_BYTES, instruction)
        }
    }

    private fun findPremiumBranch(importer: Int): Int {
        val importerCall = instructions.filter { isCall(it) && callTarget(it) == importer }.singleOrNull()
            ?: throw PatchException("Expected one calendar import dispatcher")
        val branch = importerCall - PREMIUM_BRANCH_BEFORE_IMPORT_CALL
        val check = branch - INSTRUCTION_BYTES
        if (bytes.readInt(branch) and BOOLEAN_BRANCH_MASK != BRANCH_ON_PREMIUM || !isCall(check) ||
            stringsByFunction[callTarget(check)]?.contains(PREMIUM_KEY) != true
        ) throw PatchException("Expected premium calendar import selection")
        return branch
    }

    private fun findRemainingCount(importer: Int): Int {
        val counterReaders = stringsByFunction.filterValues { COUNTER_KEY in it }.keys
        return callTargets(importer).singleOrNull { entry ->
            entry in entries && functionInstructions(entry).any { bytes.readInt(it) == MOVE_FREE_IMPORT_ALLOWANCE } &&
                callTargets(entry).any { it in counterReaders }
        } ?: throw PatchException("Expected one calendar import allowance")
    }

    private fun functionAt(address: Int): Int {
        val index = entries.binarySearch(address).let { if (it >= 0) it else -it - 2 }
        return entries.getOrNull(index) ?: throw PatchException("Could not locate Dart function")
    }

    private fun functionInstructions(entry: Int): IntProgression {
        val index = entries.binarySearch(entry)
        return (entry until entries.getOrElse(index + 1) { code.last + 1 }).step(INSTRUCTION_BYTES)
    }

    private fun isCall(address: Int) = bytes.readInt(address) and CALL_MASK == CALL

    private fun callTarget(address: Int) = address +
        (bytes.readInt(address) shl CALL_SIGN_SHIFT shr CALL_SIGN_SHIFT) * INSTRUCTION_BYTES

    private fun callTargets(entry: Int) = functionInstructions(entry).filter(::isCall).map(::callTarget).distinct()

    private fun poolOffset(address: Int): Int? {
        val instruction = bytes.readInt(address)
        if (instruction and LOAD_MASK != LOAD_UNSIGNED) return null
        val base = instruction ushr BASE_REGISTER_SHIFT and REGISTER_MASK
        val offset = (instruction ushr IMMEDIATE_SHIFT and IMMEDIATE_MASK) * Long.SIZE_BYTES
        if (base == POOL_REGISTER) return offset
        if (address - INSTRUCTION_BYTES < code.first) return null
        val add = bytes.readInt(address - INSTRUCTION_BYTES)
        if (add and ADD_MASK != ADD_IMMEDIATE || add and REGISTER_MASK != base ||
            add ushr BASE_REGISTER_SHIFT and REGISTER_MASK != POOL_REGISTER
        ) return null
        val page = add ushr IMMEDIATE_SHIFT and IMMEDIATE_MASK
        return offset + (page shl if (add and ADD_SHIFT_FLAG != 0) PAGE_SHIFT else 0)
    }
}
