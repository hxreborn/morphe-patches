/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt

private const val INSTRUCTION_BYTES = 4
private const val EQUAL = 0x54000000
private const val NOT_EQUAL = 0x54000001
private const val ALWAYS = 0x14000000
private const val LOAD_CLOSURE = 0xf9400fb0.toInt()
private const val LOAD_CLOSURE_CONTEXT = 0xb841f210.toInt()
private const val DECOMPRESS_MARKER_CONTEXT = 0x8b1c8210.toInt()
private const val LOAD_CONTEXT_MARKER = 0xb8417210.toInt()
private const val COMPARE_MARKER = 0x6b11021f
private const val ENTER_FRAME = 0xa9bf79fd.toInt()
private const val SET_FRAME = 0xaa0f03fd.toInt()
private const val RESERVE_FRAME = 0xd10181ef.toInt()
private const val INITIALIZE_SLOT = 0xf80003b6.toInt()
private const val FRAME_BYTES = 96
private const val SLOT_BYTES = 8
private const val LOAD_CALLER_FRAME = 0xf94003a1.toInt()
private const val LOAD_CALLER_ROUTINE = 0xf8400021.toInt()
private const val SAVE_SOURCE = 0xf81f83a1.toInt()
private const val LOAD_SOURCE_WORKOUTS = 0xb8400022.toInt()
private const val COMPARE_WORKOUTS_NULL = 0x6b16005f
private const val DECOMPRESS_WORKOUTS = 0x8b1c8042.toInt()
private const val SAVE_WORKOUTS = 0xf81f03a2.toInt()
private const val EMPTY_LIST_ARGUMENTS = 0xa90041ff.toInt()
private const val SAVE_EMPTY_WORKOUTS = 0xf81f03a0.toInt()
private const val LOAD_SOURCE = 0xf85f83a1.toInt()
private const val STORE_COMMIT_ROUTINE = 0xb8007001.toInt()
private const val LOAD_WORKOUTS = 0xf85f03a1.toInt()
private const val STORE_COMMIT_WORKOUTS = 0xb800b001.toInt()
private const val PASS_COMMIT = 0xaa0003e1.toInt()
private const val LOAD_MATERIALIZED_ROUTINE = 0xb8407002.toInt()
private const val DECOMPRESS_VALUE = 0x8b1c8042.toInt()
private const val SAVE_ROUTINE = 0xf81e83a2.toInt()
private const val LOAD_MATERIALIZED_WORKOUTS = 0xb840b002.toInt()
private const val LIST_FROM_ARGUMENTS = 0xa900c1e2.toInt()
private const val TRUE_ARGUMENT = 0x910082d0.toInt()
private const val STORE_ARGUMENT = 0xf90001f0.toInt()
private const val LOAD_ROUTINE = 0xf85e83a1.toInt()
private const val STORE_ROUTINE_WORKOUTS = 0xb8000020.toInt()
private const val READ_OBJECT_TAG = 0x385ff030
private const val READ_VALUE_TAG = 0x385ff011
private const val COMBINE_TAGS = 0x8a500a30.toInt()
private const val TEST_WRITE_BARRIER = 0xea5c821f.toInt()
private const val ROUTINE_AS_ARGUMENT = 0xf85e83a0.toInt()
private const val CLEAR_ROUTINE_ID = 0xb8000016.toInt()
private const val NEGATIVE_ONE_SMI = 0x12800030
private const val STORE_ROUTINE_INDEX = 0xb8000010.toInt()
private const val STORE_ROUTINE_MARKER = 0xb8000020.toInt()
private const val STORE_CALLER_ROUTINE = 0xf8000020.toInt()
private const val LEAVE_FRAME = 0xaa1d03ef.toInt()
private const val RESTORE_FRAME = 0xa8c179fd.toInt()
private const val CALLER_ROUTINE_STORE_MASK = 0xffe00fff.toInt()
private const val CALLER_ROUTINE_STORE = 0xf80003a1.toInt()
private const val FIELD_OFFSET_SHIFT = 12
private const val FIELD_OFFSET_MASK = 0x1ff
private const val FIELD_OFFSET_SIGN = 0x100
private const val FIELD_OFFSET_RANGE = 0x200

internal fun ByteArray.addRoutineCloneEditor(targets: RoutineCloneTargets, resetIdentities: Int) {
    val methods = targets.methods
    val marker = targets.workoutDetailMenu.loads("copy").single()
    val stateAllocation = methods.calls(targets.editRoutineBuilder).first()
    val savedRoutine = readInt(stateAllocation - INSTRUCTION_BYTES)
    if (savedRoutine and CALLER_ROUTINE_STORE_MASK != CALLER_ROUTINE_STORE) {
        throw PatchException("Expected routine saved before editor state allocation")
    }
    val encodedOffset = savedRoutine ushr FIELD_OFFSET_SHIFT and FIELD_OFFSET_MASK
    val callerOffset = if (encodedOffset >= FIELD_OFFSET_SIGN) encodedOffset - FIELD_OFFSET_RANGE else encodedOffset
    val stub = RoutineCloneStub()
    stub.emit(LOAD_CLOSURE, LOAD_CLOSURE_CONTEXT, DECOMPRESS_MARKER_CONTEXT, LOAD_CONTEXT_MARKER)
    stub.emit(methods.poolLoad(marker, 17))
    stub.emit(COMPARE_MARKER)
    stub.branch("clone", EQUAL, true)
    stub.jump(methods.callTarget(stateAllocation))
    stub.label("clone")
    stub.emit(ENTER_FRAME, SET_FRAME, RESERVE_FRAME)
    for (offset in SLOT_BYTES..FRAME_BYTES step SLOT_BYTES) stub.emit(dartField(INITIALIZE_SLOT, -offset))
    stub.emit(LOAD_CALLER_FRAME, dartField(LOAD_CALLER_ROUTINE, callerOffset), SAVE_SOURCE,
        dartField(LOAD_SOURCE_WORKOUTS, targets.routineWorkoutsField), COMPARE_WORKOUTS_NULL)
    stub.branch("empty", EQUAL, true)
    stub.emit(DECOMPRESS_WORKOUTS, SAVE_WORKOUTS)
    stub.branch("commit", ALWAYS, false)
    stub.label("empty")
    stub.emit(methods.poolLoadOffset(targets.workoutTypeOffset, 16))
    stub.emit(EMPTY_LIST_ARGUMENTS)
    stub.emit(methods.poolLoad(targets.emptyListArgumentsDescriptor, 4))
    stub.call(methods.callTarget(targets.emptyWorkoutListCall))
    stub.emit(SAVE_EMPTY_WORKOUTS)
    stub.label("commit")
    stub.call(targets.commitAllocation)
    stub.emit(LOAD_SOURCE, STORE_COMMIT_ROUTINE, LOAD_WORKOUTS, STORE_COMMIT_WORKOUTS, PASS_COMMIT)
    stub.call(targets.materialize.start)
    stub.emit(LOAD_MATERIALIZED_ROUTINE, DECOMPRESS_VALUE, SAVE_ROUTINE,
        LOAD_MATERIALIZED_WORKOUTS, DECOMPRESS_VALUE)
    stub.emit(methods.poolLoadOffset(targets.workoutTypeOffset, 16))
    stub.emit(LIST_FROM_ARGUMENTS, TRUE_ARGUMENT, STORE_ARGUMENT)
    stub.emit(methods.poolLoad(targets.listFromArguments, 4))
    stub.call(methods.callTarget(targets.listFromCall))
    stub.emit(LOAD_ROUTINE, dartField(STORE_ROUTINE_WORKOUTS, targets.routineWorkoutsField),
        READ_OBJECT_TAG, READ_VALUE_TAG, COMBINE_TAGS, TEST_WRITE_BARRIER)
    stub.branch("workouts_stored", EQUAL, true)
    stub.call(targets.routineListWriteBarrier)
    stub.label("workouts_stored")
    stub.emit(ROUTINE_AS_ARGUMENT)
    stub.call(resetIdentities)
    stub.emit(LOAD_ROUTINE)
    stub.emit(methods.poolLoad(marker, 0))
    stub.emit(dartField(STORE_ROUTINE_MARKER, targets.routineSharingHashField),
        READ_OBJECT_TAG, READ_VALUE_TAG, COMBINE_TAGS, TEST_WRITE_BARRIER)
    stub.branch("marker_stored", EQUAL, true)
    stub.call(targets.routineListWriteBarrier)
    stub.label("marker_stored")
    stub.emit(ROUTINE_AS_ARGUMENT, dartField(CLEAR_ROUTINE_ID, targets.routineIdField), NEGATIVE_ONE_SMI,
        dartField(STORE_ROUTINE_INDEX, targets.routineIndexField), LOAD_CALLER_FRAME,
        dartField(STORE_CALLER_ROUTINE, callerOffset), LEAVE_FRAME, RESTORE_FRAME)
    stub.jump(methods.callTarget(stateAllocation))
    val editor = stub.appendTo(this)
    writeInt(stateAllocation, Arm64.branchLink((editor - stateAllocation) / INSTRUCTION_BYTES))
}
