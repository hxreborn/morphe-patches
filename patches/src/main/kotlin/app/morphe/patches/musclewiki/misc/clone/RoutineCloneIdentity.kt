/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

private const val OFFSET_SHIFT = 12
private const val OFFSET_MASK = 0x1ff shl OFFSET_SHIFT
private const val LOAD_ROUTINE_WORKOUTS = 0xb8400001.toInt()
private const val CHECK_WORKOUTS_NULL = 0x6b16003f
private const val DECOMPRESS_WORKOUTS = 0x8b1c8021.toInt()
private const val LOAD_WORKOUT_ARRAY = 0xb840f022.toInt()
private const val DECOMPRESS_WORKOUT_ARRAY = 0x8b1c8042.toInt()
private const val LOAD_WORKOUT_COUNT = 0xb840b023.toInt()
private const val ZERO_WORKOUT_INDEX = 0xd2800004.toInt()
private const val COMPARE_WORKOUT_INDEX = 0xeb83049f.toInt()
private const val SELECT_WORKOUT_ADDRESS = 0x8b040851.toInt()
private const val LOAD_WORKOUT = 0xb840f225.toInt()
private const val DECOMPRESS_WORKOUT = 0x8b1c80a5.toInt()
private const val CLEAR_WORKOUT_FIELD = 0xb80000b6.toInt()
private const val LOAD_EXERCISES = 0xb84000a6.toInt()
private const val CHECK_EXERCISES_NULL = 0x6b1600df
private const val DECOMPRESS_EXERCISES = 0x8b1c80c6.toInt()
private const val LOAD_EXERCISE_ARRAY = 0xb840f0c7.toInt()
private const val DECOMPRESS_EXERCISE_ARRAY = 0x8b1c80e7.toInt()
private const val LOAD_EXERCISE_COUNT = 0xb840b0c8.toInt()
private const val ZERO_EXERCISE_INDEX = 0xd2800009.toInt()
private const val COMPARE_EXERCISE_INDEX = 0xeb88053f.toInt()
private const val SELECT_EXERCISE_ADDRESS = 0x8b0908f1.toInt()
private const val LOAD_EXERCISE = 0xb840f22a.toInt()
private const val DECOMPRESS_EXERCISE = 0x8b1c814a.toInt()
private const val CLEAR_EXERCISE_FIELD = 0xb8000156.toInt()
private const val LOAD_SETS = 0xb840014b.toInt()
private const val CHECK_SETS_NULL = 0x6b16017f
private const val DECOMPRESS_SETS = 0x8b1c816b.toInt()
private const val LOAD_SET_ARRAY = 0xb840f16c.toInt()
private const val DECOMPRESS_SET_ARRAY = 0x8b1c818c.toInt()
private const val LOAD_SET_COUNT = 0xb840b16d.toInt()
private const val ZERO_SET_INDEX = 0xd280000e.toInt()
private const val COMPARE_SET_INDEX = 0xeb8d05df.toInt()
private const val SELECT_SET_ADDRESS = 0x8b0e0991.toInt()
private const val LOAD_SET = 0xb840f230.toInt()
private const val DECOMPRESS_SET = 0x8b1c8210.toInt()
private const val CLEAR_SET_FIELD = 0xb8000216.toInt()
private const val INCREMENT_SET_INDEX = 0x910005ce.toInt()
private const val INCREMENT_EXERCISE_INDEX = 0x91000529.toInt()
private const val INCREMENT_WORKOUT_INDEX = 0x91000484.toInt()
private const val RETURN = 0xd65f03c0.toInt()
private const val BRANCH = 0x14000000
private const val BRANCH_IF_EQUAL = 0x54000000
private const val BRANCH_IF_GREATER_OR_EQUAL = 0x5400000a
private const val SOURCE_REGISTER = 17
private const val STORE_WORKOUT_SOURCE = 0xb80000b1.toInt()

internal fun ByteArray.addRoutineCloneIdentityReset(targets: RoutineCloneTargets): Int {
    fun field(instruction: Int, offset: Int): Int {
        require(offset in -256..255)
        return (instruction and OFFSET_MASK.inv()) or ((offset and 0x1ff) shl OFFSET_SHIFT)
    }
    val stub = RoutineCloneStub()
    stub.emit(field(LOAD_ROUTINE_WORKOUTS, targets.routineWorkoutsField), CHECK_WORKOUTS_NULL)
    stub.branch("end", BRANCH_IF_EQUAL, true)
    stub.emit(DECOMPRESS_WORKOUTS, LOAD_WORKOUT_ARRAY, DECOMPRESS_WORKOUT_ARRAY, LOAD_WORKOUT_COUNT, ZERO_WORKOUT_INDEX)
    stub.label("workoutCompare")
    stub.emit(COMPARE_WORKOUT_INDEX)
    stub.branch("end", BRANCH_IF_GREATER_OR_EQUAL, true)
    stub.emit(SELECT_WORKOUT_ADDRESS, LOAD_WORKOUT, DECOMPRESS_WORKOUT,
        field(CLEAR_WORKOUT_FIELD, targets.workoutIdField), field(CLEAR_WORKOUT_FIELD, targets.workoutIndexField),
        field(CLEAR_WORKOUT_FIELD, targets.workoutSharingHashField), field(CLEAR_WORKOUT_FIELD, targets.workoutDateField),
        field(CLEAR_WORKOUT_FIELD, targets.workoutLastTrackedField))
    stub.emit(targets.methods.poolLoadOffset(targets.userCreatedSourceOffset, SOURCE_REGISTER))
    stub.emit(field(STORE_WORKOUT_SOURCE, targets.workoutSourceField), field(LOAD_EXERCISES, targets.workoutExercisesField),
        CHECK_EXERCISES_NULL)
    stub.branch("workoutNext", BRANCH_IF_EQUAL, true)
    stub.emit(DECOMPRESS_EXERCISES, LOAD_EXERCISE_ARRAY, DECOMPRESS_EXERCISE_ARRAY, LOAD_EXERCISE_COUNT, ZERO_EXERCISE_INDEX)
    stub.label("exerciseCompare")
    stub.emit(COMPARE_EXERCISE_INDEX)
    stub.branch("workoutNext", BRANCH_IF_GREATER_OR_EQUAL, true)
    stub.emit(SELECT_EXERCISE_ADDRESS, LOAD_EXERCISE, DECOMPRESS_EXERCISE,
        field(CLEAR_EXERCISE_FIELD, targets.exerciseIdField), field(CLEAR_EXERCISE_FIELD, targets.exerciseSyncIdField),
        field(CLEAR_EXERCISE_FIELD, targets.exerciseWorkoutField))
    targets.exerciseBlockFields.forEach { stub.emit(field(CLEAR_EXERCISE_FIELD, it)) }
    stub.emit(field(LOAD_SETS, targets.exerciseSetsField), CHECK_SETS_NULL)
    stub.branch("exerciseNext", BRANCH_IF_EQUAL, true)
    stub.emit(DECOMPRESS_SETS, LOAD_SET_ARRAY, DECOMPRESS_SET_ARRAY, LOAD_SET_COUNT, ZERO_SET_INDEX)
    stub.label("setCompare")
    stub.emit(COMPARE_SET_INDEX)
    stub.branch("exerciseNext", BRANCH_IF_GREATER_OR_EQUAL, true)
    stub.emit(SELECT_SET_ADDRESS, LOAD_SET, DECOMPRESS_SET, field(CLEAR_SET_FIELD, targets.setIdField),
        field(CLEAR_SET_FIELD, targets.setExerciseField), INCREMENT_SET_INDEX)
    stub.branch("setCompare", BRANCH, false)
    stub.label("exerciseNext")
    stub.emit(INCREMENT_EXERCISE_INDEX)
    stub.branch("exerciseCompare", BRANCH, false)
    stub.label("workoutNext")
    stub.emit(INCREMENT_WORKOUT_INDEX)
    stub.branch("workoutCompare", BRANCH, false)
    stub.label("end")
    stub.emit(RETURN)
    return stub.appendTo(this)
}
