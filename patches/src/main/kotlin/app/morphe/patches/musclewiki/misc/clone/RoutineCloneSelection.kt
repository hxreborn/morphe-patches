/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.dart.appendNativeCode
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val CONTEXT_LOAD_AFTER_CALL = 8
private const val CONTEXT_LOAD_MASK = 0xffe00fff.toInt()
private const val CONTEXT_LOAD = 0xf84003a3.toInt()
private const val OFFSET_SHIFT = 12
private const val OFFSET_MASK = 0x1ff
private const val LOAD_SELECTED_VALUE = 0xf94001f0.toInt()
private const val COMPARE_SELECTED_VALUE = 0x6b11021f
private const val BRANCH_IF_COPY = 0x54000040
private const val FRAME_ENTRY = 0xa9bf79fd.toInt()
private const val SET_FRAME = 0xaa0f03fd.toInt()
private const val RESERVE_FRAME = 0xd10041ef.toInt()
private const val INITIALIZE_CONTEXT = 0xf81f83b6.toInt()
private const val INITIALIZE_SPARE = 0xf81f03b6.toInt()
private const val LOAD_CALLER_IN_CONTEXT = 0xf94003a2.toInt()
private const val LOAD_CALLER_CONTEXT = 0xf8400040.toInt()
private const val SAVE_CONTEXT = 0xf81f83a0.toInt()
private const val LOAD_CONTEXT = 0xf85f83a2.toInt()
private const val READ_WIDGET = 0xb840f041.toInt()
private const val WRITE_WIDGET = 0xb800f001.toInt()
private const val READ_BUILD_CONTEXT = 0xb8413041.toInt()
private const val WRITE_BUILD_CONTEXT = 0xb8013001.toInt()
private const val WRITE_COPY_MARKER = 0xb8017001.toInt()
private const val LOAD_CALLER_FRAME = 0xf94003a1.toInt()
private const val REPLACE_CALLER_CONTEXT = 0xf8000020.toInt()
private const val RETURN_TRUE = 0x910082c0.toInt()
private const val LEAVE_FRAME = 0xaa1d03ef.toInt()
private const val RESTORE_FRAME = 0xa8c179fd.toInt()
private const val RETURN = 0xd65f03c0.toInt()
private const val CONTEXT_FIELDS = 3
private const val GENERATED_CONTEXT_FIELDS = 4
private const val COPY_MARKER_REGISTER = 1
private const val COMPARE_MARKER_REGISTER = 17

internal fun ByteArray.addRoutineCloneSelection(targets: RoutineCloneTargets) {
    val methods = targets.methods
    val marker = targets.workoutDetailMenu.loads("copy").single()
    val allocateContextCall = methods.calls(targets.createGeneratedRoutine).first()
    if (readInt(allocateContextCall - INSTRUCTION_BYTES) != Arm64.moveWideZero(1, GENERATED_CONTEXT_FIELDS)) {
        throw PatchException("Expected generated routine context allocation")
    }
    val allocateContext = methods.callTarget(allocateContextCall)
    listOf(targets.routineDetailAction, targets.routineListAction).forEach { action ->
        val edit = action.loads("edit").single()
        val compare = methods.firstCallAfter(edit, action.end)
        val originalTarget = methods.callTarget(compare)
        val load = readInt(compare + CONTEXT_LOAD_AFTER_CALL)
        if (load and CONTEXT_LOAD_MASK != CONTEXT_LOAD) {
            throw PatchException("Expected routine action context after edit comparison")
        }
        val encodedOffset = load ushr OFFSET_SHIFT and OFFSET_MASK
        val code = mutableListOf(LOAD_SELECTED_VALUE)
        code += methods.poolLoad(marker, COMPARE_MARKER_REGISTER)
        code += listOf(COMPARE_SELECTED_VALUE, BRANCH_IF_COPY)
        val originalBranch = code.size
        code += 0
        code += listOf(FRAME_ENTRY, SET_FRAME, RESERVE_FRAME, INITIALIZE_CONTEXT, INITIALIZE_SPARE,
            LOAD_CALLER_IN_CONTEXT, LOAD_CALLER_CONTEXT or (encodedOffset shl OFFSET_SHIFT), SAVE_CONTEXT,
            Arm64.moveWideZero(1, CONTEXT_FIELDS))
        val contextCall = code.size
        code += 0
        code += listOf(LOAD_CONTEXT, READ_WIDGET, WRITE_WIDGET, READ_BUILD_CONTEXT, WRITE_BUILD_CONTEXT)
        code += methods.poolLoad(marker, COPY_MARKER_REGISTER)
        code += listOf(WRITE_COPY_MARKER, LOAD_CALLER_FRAME,
            REPLACE_CALLER_CONTEXT or (encodedOffset shl OFFSET_SHIFT), RETURN_TRUE, LEAVE_FRAME, RESTORE_FRAME, RETURN)
        val body = ByteBuffer.allocate(code.size * INSTRUCTION_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        code.forEach(body::putInt)
        val start = appendNativeCode(body.array())
        val originalAddress = start + originalBranch * INSTRUCTION_BYTES
        writeInt(originalAddress, Arm64.branch((originalTarget - originalAddress) / INSTRUCTION_BYTES))
        val contextAddress = start + contextCall * INSTRUCTION_BYTES
        writeInt(contextAddress, Arm64.branchLink((allocateContext - contextAddress) / INSTRUCTION_BYTES))
        writeInt(compare, Arm64.branchLink((start - compare) / INSTRUCTION_BYTES))
    }
}
