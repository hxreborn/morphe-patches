/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.writeInt

private const val INSTRUCTION_BYTES = 4
private const val NOT_EQUAL = 0x54000001
private const val LOAD_ROUTINE_INDEX = 0xb8400050.toInt()
private const val CHECK_NEGATIVE_ONE_SMI = 0x31000a1f
private const val LOAD_ROUTINE_MARKER = 0xb8400050.toInt()
private const val COMPARE_MARKER = 0x6b11021f
private const val ENTER_FRAME = 0xa9bf79fd.toInt()
private const val SET_FRAME = 0xaa0f03fd.toInt()
private const val RESERVE_FRAME = 0xd10081ef.toInt()
private const val INITIALIZE_SLOT = 0xf80003b6.toInt()
private const val FRAME_BYTES = 32
private const val SLOT_BYTES = 8
private const val SAVE_REPOSITORY = 0xf81f83a1.toInt()
private const val SAVE_ROUTINE = 0xf81f03a2.toInt()
private const val CLEAR_ROUTINE_FIELD = 0xb8000056.toInt()
private const val ROUTINE_AS_ARGUMENT = 0xaa0203e0.toInt()
private const val LOAD_ROUTINE = 0xf85f03a2.toInt()
private const val STORE_COMMIT_ROUTINE = 0xb8007002.toInt()
private const val LOAD_WORKOUTS = 0xb8400043.toInt()
private const val STORE_COMMIT_WORKOUTS = 0xb800b003.toInt()
private const val PASS_COMMIT = 0xaa0003e2.toInt()
private const val LOAD_REPOSITORY = 0xf85f83a1.toInt()
private const val LEAVE_FRAME = 0xaa1d03ef.toInt()
private const val RESTORE_FRAME = 0xa8c179fd.toInt()

internal fun ByteArray.addRoutineCloneSave(targets: RoutineCloneTargets, resetIdentities: Int) {
    val methods = targets.methods
    val marker = targets.workoutDetailMenu.loads("copy").single()
    val call = methods.calls(targets.createRoutineCaller).single { methods.callTarget(it) == targets.createRoutine.start }
    val dispatch = RoutineCloneStub()
    dispatch.emit(dartField(LOAD_ROUTINE_INDEX, targets.routineIndexField), CHECK_NEGATIVE_ONE_SMI)
    dispatch.branch("original", NOT_EQUAL, true)
    dispatch.emit(dartField(LOAD_ROUTINE_MARKER, targets.routineSharingHashField))
    dispatch.emit(methods.poolLoad(marker, 17))
    dispatch.emit(COMPARE_MARKER)
    dispatch.branch("original", NOT_EQUAL, true)
    dispatch.jump(targets.createRoutineCaller.start)
    dispatch.label("original")
    dispatch.jump(methods.callTarget(targets.updateSaveCall))
    val dispatchEntry = dispatch.appendTo(this)
    writeInt(targets.updateSaveCall, Arm64.branchLink((dispatchEntry - targets.updateSaveCall) / INSTRUCTION_BYTES))

    val stub = RoutineCloneStub()
    stub.emit(dartField(LOAD_ROUTINE_INDEX, targets.routineIndexField), CHECK_NEGATIVE_ONE_SMI)
    stub.branch("original", NOT_EQUAL, true)
    stub.emit(dartField(LOAD_ROUTINE_MARKER, targets.routineSharingHashField))
    stub.emit(methods.poolLoad(marker, 17))
    stub.emit(COMPARE_MARKER)
    stub.branch("original", NOT_EQUAL, true)
    stub.emit(ENTER_FRAME, SET_FRAME, RESERVE_FRAME)
    for (offset in SLOT_BYTES..FRAME_BYTES step SLOT_BYTES) stub.emit(dartField(INITIALIZE_SLOT, -offset))
    stub.emit(SAVE_REPOSITORY, SAVE_ROUTINE, dartField(CLEAR_ROUTINE_FIELD, targets.routineIdField),
        dartField(CLEAR_ROUTINE_FIELD, targets.routineIndexField),
        dartField(CLEAR_ROUTINE_FIELD, targets.routineSharingHashField), ROUTINE_AS_ARGUMENT)
    stub.call(resetIdentities)
    stub.call(targets.commitAllocation)
    stub.emit(LOAD_ROUTINE, STORE_COMMIT_ROUTINE, dartField(LOAD_WORKOUTS, targets.routineWorkoutsField),
        STORE_COMMIT_WORKOUTS, PASS_COMMIT, LOAD_REPOSITORY, LEAVE_FRAME, RESTORE_FRAME)
    stub.jump(targets.createGeneratedRoutine.start)
    stub.label("original")
    stub.jump(targets.createRoutine.start)
    val entry = stub.appendTo(this)
    writeInt(call, Arm64.branchLink((entry - call) / INSTRUCTION_BYTES))
}
