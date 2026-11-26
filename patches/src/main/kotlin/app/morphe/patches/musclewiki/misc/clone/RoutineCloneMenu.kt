/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.dart.NativeFunction
import app.morphe.patches.shared.misc.dart.appendNativeCode
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val GROW_BRANCH_BEFORE_CALL = 8
private const val INDEX_STORE_BEFORE_CALL = 20
private const val RETURN_SEQUENCE_BYTES = 12
private const val SAVE_INDEX_MASK = 0xffe00fff.toInt()
private const val SAVE_INDEX_INSTRUCTION = 0xf80003a3.toInt()
private const val OFFSET_SHIFT = 12
private const val OFFSET_MASK = 0x1ff
private const val OFFSET_SIGN = 0x100
private const val OFFSET_RANGE = 0x200
private const val BRANCH_NOT_EQUAL_OVER_CALL = 0x54000061
private const val MOVE_LIST_ARGUMENT = 0xaa0003e1.toInt()
private const val RETURN_LIST = 0xaa0203e0.toInt()
private const val LEAVE_FRAME = 0xaa1d03ef.toInt()
private const val RESTORE_FRAME = 0xa8c179fd.toInt()
private const val RETURN = 0xd65f03c0.toInt()
private const val PROLOGUE = 0xa9bf79fd.toInt()
private const val SET_FRAME = 0xaa0f03fd.toInt()
private const val RESERVE_FRAME = 0xd10081ef.toInt()
private const val SAVE_LIST = 0xf81f83a1.toInt()
private const val INITIALIZE_ITEM = 0xf81f03b6.toInt()
private const val INITIALIZE_SPARE = 0xf81e83b6.toInt()
private const val INITIALIZE_LAST = 0xf81e03b6.toInt()
private const val LOAD_CALLER_FRAME_IN_CONTEXT = 0xf94003a2.toInt()
private const val LOAD_CONTEXT_FROM_CALLER = 0xf9400842.toInt()
private const val NULL_RECEIVER = 0xaa1603e1.toInt()
private const val SAVE_ITEM = 0xf81f03a0.toInt()
private const val LOAD_LIST = 0xf85f83a1.toInt()
private const val LOAD_LENGTH = 0xb840b022.toInt()
private const val LOAD_STORAGE = 0xb840f023.toInt()
private const val DECOMPRESS_STORAGE = 0x8b1c8063.toInt()
private const val LOAD_CAPACITY = 0xb840b063.toInt()
private const val COMPARE_LENGTH = 0x6b03005f
private const val SKIP_CALL_IF_NOT_EQUAL = 0x54000041
private const val RELOAD_LIST = 0xf85f83a2.toInt()
private const val READ_LENGTH = 0xb840b043.toInt()
private const val INCREMENT_LENGTH = 0x11000864
private const val WRITE_LENGTH = 0xb800b044.toInt()
private const val UNTAG_INDEX = 0x9341fc63.toInt()
private const val READ_STORAGE = 0xb840f041.toInt()
private const val DECOMPRESS_ARRAY = 0x8b1c8021.toInt()
private const val LOAD_ITEM = 0xf85f03a0.toInt()
private const val ARRAY_INDEX = 0x8b030839.toInt()
private const val ARRAY_DATA = 0x91003f39.toInt()
private const val WRITE_ITEM = 0xb9000320.toInt()
private const val SKIP_BARRIER_FOR_SMI = 0x360000e0
private const val READ_ARRAY_TAG = 0x385ff030
private const val READ_ITEM_TAG = 0x385ff011
private const val COMBINE_TAGS = 0x8a500a30.toInt()
private const val TEST_WRITE_BARRIER = 0xea5c821f.toInt()
private const val SKIP_BARRIER = 0x54000040
private const val LOAD_CALLER_FRAME = 0xf94003a1.toInt()
private const val LOAD_CALLER_INDEX = 0xf8400022.toInt()
private const val NEXT_CALLER_INDEX = 0x91000442.toInt()
private const val STORE_CALLER_INDEX = 0xf8000022.toInt()
private const val RETURN_NULL = 0xaa1603e0.toInt()

internal fun ByteArray.addRoutineCloneMenu(targets: RoutineCloneTargets) {
    val methods = targets.methods
    val duplicate = targets.workoutDetailMenu.loads("duplicate").single()
    val copy = targets.workoutDetailMenu.loads("copy").single()
    val icon = methods.poolLoads(targets.workoutDetailMenu).filter { it > copy && it < duplicate }.singleOrNull()
        ?: throw PatchException("Expected workout duplication icon")
    listOf(targets.routineDetailMenu, targets.routineListMenu).forEach { menu ->
        val returnSite = (menu.start until menu.end - RETURN_SEQUENCE_BYTES step INSTRUCTION_BYTES).singleOrNull { address ->
            readInt(address) == RETURN_LIST && readInt(address + INSTRUCTION_BYTES) == LEAVE_FRAME &&
                readInt(address + 2 * INSTRUCTION_BYTES) == RESTORE_FRAME &&
                readInt(address + 3 * INSTRUCTION_BYTES) == RETURN
        } ?: throw PatchException("Expected routine menu return")
        val calls = methods.calls(menu).filter { it < returnSite }
        if (calls.size < 2) throw PatchException("Expected routine menu list growth and write barrier")
        val growCall = calls[calls.lastIndex - 1]
        val barrierCall = calls.last()
        val savedIndex = readInt(growCall - INDEX_STORE_BEFORE_CALL)
        if (savedIndex and SAVE_INDEX_MASK != SAVE_INDEX_INSTRUCTION ||
            readInt(growCall - GROW_BRANCH_BEFORE_CALL) != BRANCH_NOT_EQUAL_OVER_CALL ||
            readInt(growCall - INSTRUCTION_BYTES) != MOVE_LIST_ARGUMENT) {
            throw PatchException("Expected routine menu list growth sequence")
        }
        val encodedOffset = savedIndex ushr OFFSET_SHIFT and OFFSET_MASK
        val offset = if (encodedOffset >= OFFSET_SIGN) encodedOffset - OFFSET_RANGE else encodedOffset
        val code = mutableListOf(PROLOGUE, SET_FRAME, RESERVE_FRAME, SAVE_LIST, INITIALIZE_ITEM,
            INITIALIZE_SPARE, INITIALIZE_LAST, LOAD_CALLER_FRAME_IN_CONTEXT, LOAD_CONTEXT_FROM_CALLER, NULL_RECEIVER)
        code += methods.poolLoad(copy, 3)
        code += methods.poolLoad(icon, 5)
        code += methods.poolLoad(duplicate, 6)
        val menuCall = code.size
        code += 0
        code += listOf(SAVE_ITEM, LOAD_LIST, LOAD_LENGTH, LOAD_STORAGE, DECOMPRESS_STORAGE, LOAD_CAPACITY,
            COMPARE_LENGTH, SKIP_CALL_IF_NOT_EQUAL)
        val firstGrow = code.size
        code += 0
        code += listOf(RELOAD_LIST, READ_LENGTH, INCREMENT_LENGTH, WRITE_LENGTH, UNTAG_INDEX, READ_STORAGE,
            DECOMPRESS_ARRAY, LOAD_ITEM, ARRAY_INDEX, ARRAY_DATA, WRITE_ITEM, SKIP_BARRIER_FOR_SMI,
            READ_ARRAY_TAG, READ_ITEM_TAG, COMBINE_TAGS, TEST_WRITE_BARRIER, SKIP_BARRIER)
        val barrier = code.size
        code += 0
        code += listOf(LOAD_LIST, LOAD_LENGTH, LOAD_STORAGE, DECOMPRESS_STORAGE, LOAD_CAPACITY, COMPARE_LENGTH,
            SKIP_CALL_IF_NOT_EQUAL)
        val secondGrow = code.size
        code += 0
        code += listOf(LOAD_CALLER_FRAME, LOAD_CALLER_INDEX or ((offset and OFFSET_MASK) shl OFFSET_SHIFT),
            NEXT_CALLER_INDEX, STORE_CALLER_INDEX or ((offset and OFFSET_MASK) shl OFFSET_SHIFT),
            RETURN_NULL, LEAVE_FRAME, RESTORE_FRAME, RETURN)
        val body = ByteBuffer.allocate(code.size * INSTRUCTION_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        code.forEach(body::putInt)
        val start = appendNativeCode(body.array())
        fun call(index: Int, target: Int) {
            val address = start + index * INSTRUCTION_BYTES
            writeInt(address, Arm64.branchLink((target - address) / INSTRUCTION_BYTES))
        }
        call(menuCall, targets.menuItem)
        call(firstGrow, methods.callTarget(growCall))
        call(secondGrow, methods.callTarget(growCall))
        call(barrier, methods.callTarget(barrierCall))
        writeInt(growCall - GROW_BRANCH_BEFORE_CALL, Arm64.NOP)
        writeInt(growCall, Arm64.branchLink((start - growCall) / INSTRUCTION_BYTES))
    }
}
