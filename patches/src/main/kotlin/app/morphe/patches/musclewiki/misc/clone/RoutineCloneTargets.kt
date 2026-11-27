/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.dart.NativeFunction
import app.morphe.patches.shared.misc.dart.NativeMethods
import app.morphe.patches.shared.readInt

private const val INSTRUCTION_BYTES = 4
private const val FIELD_LOAD_WINDOW = 20
private const val UNSCALED_WORD_LOAD = 0xb8400000.toInt()
private const val UNSCALED_LOAD_MASK = 0xffe00c00.toInt()
private const val BASE_REGISTER_SHIFT = 5
private const val REGISTER_MASK = 31
private const val FRAME_REGISTER = 29
private const val FIELD_OFFSET_SHIFT = 12
private const val FIELD_OFFSET_MASK = 511
private const val FIELD_OFFSET_SIGN = 256
private const val FIELD_OFFSET_RANGE = 512
private const val UNSCALED_WORD_STORE = 0xb8000000.toInt()
private const val COMPARE_WORD_MASK = 0xffe0fc1f.toInt()
private const val COMPARE_WORD = 0x6b00001f
private const val COMPARE_RIGHT_REGISTER_SHIFT = 16

internal class RoutineCloneTargets(bytes: ByteArray) {
    val methods = NativeMethods(bytes)
    private val callers = buildMap<Int, MutableSet<NativeFunction>> {
        methods.all().forEach { function ->
            methods.calls(function).forEach { call ->
                getOrPut(methods.callTarget(call)) { mutableSetOf() }.add(function)
            }
        }
    }
    private val validator = methods.single(
        "Generated routine has an empty workout list.",
        "Every generated workout must contain exercises.",
    )
    val complete = callers[validator.start].only("generated routine validator caller")
    private val copyMethods = methods.calls(complete).map(methods::callTarget).distinct().filter { target ->
        val matches = callers[target].orEmpty()
        matches.size == 2 && complete in matches
    }
    val routineClone = copyMethods.filter { target ->
        callers[target].orEmpty().filterNot { it == complete }.any { callers[it.start].orEmpty().size == 1 }
    }.only("routine model clone")
    val workoutClone = copyMethods.filter { target ->
        callers[target].orEmpty().filterNot { it == complete }.any { callers[it.start].isNullOrEmpty() }
    }.only("workout model clone")
    val materialize = callers[routineClone].orEmpty().filterNot { it == complete }.only("routine materializer")
    private val validatorCall = methods.calls(complete).single { methods.callTarget(it) == validator.start }
    private val nullRegister = methods.instructionsBeforeControlFlowAfter(validatorCall, complete.end)
        .map(methods.bytes::readInt).filter { it and COMPARE_WORD_MASK == COMPARE_WORD }
        .map { it ushr COMPARE_RIGHT_REGISTER_SHIFT and REGISTER_MASK }.only("routine validator null comparison")
    private val validCommitBranch = methods.equalBranchTargetAfter(validatorCall, complete.end)
    val commitAllocation = methods.callTarget(methods.firstCallAfter(validCommitBranch - INSTRUCTION_BYTES, complete.end))
    private val materializeCalls = methods.calls(materialize)
    private val growableArguments = methods.snapshot.arrayPoolOffsetsContaining("growable")
    val listFromArguments = methods.poolLoads(materialize).filter { methods.poolOffset(it) in growableArguments }
        .only("workout list copy arguments")
    val listFromCall = methods.firstCallAfter(listFromArguments, materialize.end)
    private val completePools = methods.poolLoads(complete).groupBy(methods::poolOffset)
    private val materializePools = methods.poolLoads(materialize).groupBy(methods::poolOffset)
    val workoutTypeOffset = materializePools.keys.intersect(completePools.keys)
        .intersect(methods.snapshot.typeArgumentPoolOffsets()).only("workout list type arguments")
    private val cloneRoutineCall = materializeCalls.single { methods.callTarget(it) == routineClone }
    private val routineWorkoutsStore = methods.instructionsBeforeControlFlowAfter(cloneRoutineCall, materialize.end)
        .filter { address ->
            val instruction = methods.bytes.readInt(address)
            instruction and UNSCALED_LOAD_MASK == UNSCALED_WORD_STORE &&
                instruction ushr BASE_REGISTER_SHIFT and REGISTER_MASK != FRAME_REGISTER
        }.only("routine workout list store")
    val routineWorkoutsField = fieldOffset(methods.bytes.readInt(routineWorkoutsStore))
    private val afterRoutineWriteBarrier = methods.equalBranchTargetAfter(routineWorkoutsStore, materialize.end)
    val routineListWriteBarrier = methods.callTarget(methods.firstCallAfter(routineWorkoutsStore, afterRoutineWriteBarrier))
    val createGeneratedRoutine = callers[materialize.start].only("generated routine repository method")
    private val routineChanges = methods.matching("male", "static:")
    val createRoutine = routineChanges.filter { "" !in it.stringLoads.values }.only("routine creation method")
    private val updateRoutine = routineChanges.filter { "" in it.stringLoads.values }.only("routine update method")
    private val updateCallers = callers[updateRoutine.start].orEmpty().flatMap { callers[it.start].orEmpty() }.toSet()
    val createRoutineCaller = callers[createRoutine.start].orEmpty().filter { caller ->
        callers[caller.start].orEmpty().any { it in updateCallers }
    }.only("routine creation caller")
    val finalizeRoutine = callers[createRoutineCaller.start].only("routine finalizer")
    private val finalizeTargets = methods.calls(finalizeRoutine).map(methods::callTarget).toSet()
    private val emptyListArguments = completePools.filterKeys {
        it in materializePools && it != workoutTypeOffset && it !in growableArguments
    }.values.flatten().filter { load ->
        methods.callTarget(methods.firstCallAfter(load, complete.end)) in finalizeTargets
    }
    val emptyListArgumentsDescriptor = emptyListArguments.only("empty workout list constructor arguments")
    val emptyWorkoutListCall = methods.firstCallAfter(emptyListArgumentsDescriptor, complete.end)
    private val updateActionOffsets = methods.snapshot.instancePoolOffsetsContaining("update")
    private val editAction = methods.poolLoads(finalizeRoutine).filter { methods.poolOffset(it) in updateActionOffsets }
        .only("routine update action")
    private val editActionReference = methods.references(methods.poolOffset(editAction)).filter { address ->
        val load = methods.bytes.readInt(address)
        val store = methods.bytes.readInt(address + INSTRUCTION_BYTES)
        store and UNSCALED_LOAD_MASK == UNSCALED_WORD_STORE &&
            store and REGISTER_MASK == load and REGISTER_MASK &&
            store ushr BASE_REGISTER_SHIFT and REGISTER_MASK != FRAME_REGISTER
    }.only("routine edit screen action")
    val editRoutineBuilder = methods.containing(editActionReference)
    val updateSaveCall = methods.calls(finalizeRoutine).filter {
        methods.containing(methods.callTarget(it)) in callers[updateRoutine.start].orEmpty()
    }.only("routine update save call")
    private val workoutCopy = methods.containing(workoutClone)
    private val workoutCalls = methods.calls(workoutCopy).map(methods::callTarget).filter(methods::isFunctionEntry).toSet()
    private val workoutCreator = methods.matching("male").filter { function ->
        methods.calls(function).map(methods::callTarget).count { it in workoutCalls } == 2
    }.only("workout creation helper")
    private val routineSerializer = serializer(routineClone, "workouts")
    private val workoutSerializer = serializer(workoutClone, "exercises")
    val routineIdField = fieldAfterKey(routineSerializer, "id")
        ?: throw PatchException("Expected routine ID field")
    val routineIndexField = fieldAfterKey(routineSerializer, "index")
        ?: throw PatchException("Expected routine index field")
    val routineSharingHashField = fieldAfterKey(routineSerializer, "sharing_hash")
        ?: throw PatchException("Expected routine sharing hash field")
    val workoutIndexField = fieldAfterKey(workoutSerializer, "index")
        ?: throw PatchException("Expected workout index field")
    val workoutIdField = fieldAfterKey(workoutSerializer, "id")
        ?: throw PatchException("Expected workout ID field")
    val workoutExercisesField = fieldAfterKey(workoutSerializer, "exercises")
        ?: throw PatchException("Expected workout exercises field")
    val workoutSharingHashField = fieldAfterKey(workoutSerializer, "sharing_hash")
        ?: throw PatchException("Expected workout sharing hash field")
    val workoutDateField = fieldAfterKey(workoutSerializer, "date")
        ?: throw PatchException("Expected workout date field")
    val workoutLastTrackedField = fieldAfterKey(workoutSerializer, "last_tracked")
        ?: throw PatchException("Expected workout last-tracked field")
    private val exerciseSerializer = methods.matching("active_sync_id", "exerciseDetail", "sets", "workout")
        .filter { fieldAfterKey(it, "active_sync_id") != null }.only("exercise serializer")
    private val exerciseIdCandidates = exerciseSerializer.loads("id")
        .mapNotNull { fieldAfterLoad(exerciseSerializer, it) }.distinct()
    val exerciseSyncIdField = fieldAfterKey(exerciseSerializer, "active_sync_id")
        ?: throw PatchException("Expected exercise sync ID field")
    val exerciseWorkoutField = fieldAfterKey(exerciseSerializer, "workout")
        ?: throw PatchException("Expected exercise workout field")
    val exerciseSetsField = fieldAfterKey(exerciseSerializer, "sets")
        ?: throw PatchException("Expected exercise sets field")
    val workoutSourceField = firstFieldAfterKey(workoutSerializer, "source")
        ?: throw PatchException("Expected workout source field")
    private val userCreatedSources = methods.snapshot.instancePoolOffsetsContaining("UC")
    private val workoutCopyFactory = methods.matching("copy", " - ").filter { function ->
        methods.poolLoads(function).any { methods.poolOffset(it) in userCreatedSources }
    }.only("workout copy factory")
    val userCreatedSourceOffset = methods.poolLoads(workoutCopyFactory).map(methods::poolOffset)
        .filter { it in userCreatedSources }.distinct().only("user created workout source")
    private val exerciseGroupKeyField = firstFieldAfterKey(exerciseSerializer, "group_key")
        ?: throw PatchException("Expected exercise group key field")
    private val exerciseBlockOrderField = firstFieldAfterKey(exerciseSerializer, "block_order")
        ?: throw PatchException("Expected exercise block order field")
    val exerciseBlockFields = (listOf(exerciseGroupKeyField, exerciseBlockOrderField) +
        fieldsAfterKey(exerciseSerializer, "block")).distinct()
    private val workoutIdentityFields = listOf(workoutIdField, workoutSharingHashField, workoutDateField, workoutLastTrackedField)
    private val resetCandidates = methods.calls(workoutCreator).map(methods::callTarget).distinct()
        .filter(methods::isFunctionEntry).flatMap { target ->
            val function = methods.containing(target)
            val fields = (function.start until function.end step INSTRUCTION_BYTES)
                .map { methods.bytes.readInt(it) }.filter {
                    it and UNSCALED_LOAD_MASK == UNSCALED_WORD_STORE && it and REGISTER_MASK == nullRegister
                }
                .map(::fieldOffset)
            exerciseIdCandidates.mapNotNull { exerciseId ->
                val remaining = fields.toMutableList()
                if ((workoutIdentityFields + exerciseId).all(remaining::remove) && remaining.size == 1) {
                    Triple(target, exerciseId, remaining.single())
                } else null
            }
        }
    private val resetCandidate = resetCandidates.only("workout, exercise and set identity resets")
    val makeNew = resetCandidate.first
    val exerciseIdField = resetCandidate.second
    val setIdField = resetCandidate.third
    private val setSerializer = methods.matching("id", "order", "range", "load", "exercise", "complete")
        .filter { fieldBeforeKey(it, "id") == setIdField || fieldAfterKey(it, "id") == setIdField }
        .only("set serializer")
    val setExerciseField = (if (fieldBeforeKey(setSerializer, "id") == setIdField) {
        fieldBeforeKey(setSerializer, "exercise")
    } else {
        fieldAfterKey(setSerializer, "exercise")
    })
        ?: throw PatchException("Expected set exercise field")
    private val menus = methods.matching("edit", "share", "delete", "export", "assets/icons/delete.png")
    val routineDetailMenu = menus.filter {
        "duplicate" !in it.stringLoads.values && "view_detail" !in it.stringLoads.values
    }.only("routine detail menu")
    val routineListMenu = menus.filter { "view_detail" in it.stringLoads.values }.only("routine list menu")
    val menuItem = routineDetailMenu.loads("edit").map {
        methods.callTarget(methods.firstCallAfter(it, routineDetailMenu.end))
    }.distinct().only("routine menu item builder")
    val workoutDetailMenu = menus.filter { menu ->
        "duplicate" in menu.stringLoads.values && methods.calls(menu).any { methods.callTarget(it) == menuItem }
    }.only("workout detail menu")
    private val actions = methods.matching("edit", "share", "delete", "export").filter {
        "assets/icons/delete.png" !in it.stringLoads.values && "copy" !in it.stringLoads.values
    }
    val routineDetailAction = actions.filter { "detail" !in it.stringLoads.values }.only("routine detail action")
    val routineListAction = actions.filter { "detail" in it.stringLoads.values }.only("routine list action")

    private fun serializer(clone: Int, children: String): NativeFunction {
        val wrappers = methods.calls(methods.containing(clone)).map(methods::callTarget).toSet()
        return methods.matching("id", children, "notes", "sharing_hash", "index").filter { function ->
            callers[function.start].orEmpty().any { it.start in wrappers } && fieldAfterKey(function, "index") != null
        }.only("$children serializer")
    }

    private fun fieldAfterKey(function: NativeFunction, key: String): Int? {
        val load = function.loads(key).singleOrNull() ?: return null
        return fieldAfterLoad(function, load)
    }

    private fun firstFieldAfterKey(function: NativeFunction, key: String): Int? {
        val load = function.loads(key).singleOrNull() ?: return null
        return fieldsAfterLoad(function, load).firstOrNull()
    }

    private fun fieldsAfterKey(function: NativeFunction, key: String): List<Int> {
        val load = function.loads(key).singleOrNull() ?: throw PatchException("Expected one $key key load")
        return fieldsAfterLoad(function, load)
    }

    private fun fieldsAfterLoad(function: NativeFunction, load: Int): List<Int> {
        val start = load + INSTRUCTION_BYTES
        return fieldsIn(start until minOf(function.end, start + FIELD_LOAD_WINDOW))
    }

    private fun fieldAfterLoad(function: NativeFunction, load: Int): Int? =
        fieldsAfterLoad(function, load).singleOrNull()

    private fun fieldBeforeKey(function: NativeFunction, key: String): Int? {
        return function.loads(key).mapNotNull { load ->
            val start = methods.poolLoadStart(load)
            fieldsIn(maxOf(function.start, start - FIELD_LOAD_WINDOW) until start).singleOrNull()
        }.distinct().singleOrNull()
    }

    private fun fieldsIn(range: IntRange): List<Int> = (range.first..range.last step INSTRUCTION_BYTES)
            .map { methods.bytes.readInt(it) }
            .filter { instruction ->
                instruction and UNSCALED_LOAD_MASK == UNSCALED_WORD_LOAD &&
                    instruction ushr BASE_REGISTER_SHIFT and REGISTER_MASK != FRAME_REGISTER
            }.map(::fieldOffset).distinct()

    private fun fieldOffset(instruction: Int): Int {
        val offset = instruction ushr FIELD_OFFSET_SHIFT and FIELD_OFFSET_MASK
        return if (offset >= FIELD_OFFSET_SIGN) offset - FIELD_OFFSET_RANGE else offset
    }
}

private fun <T> Collection<T>?.only(name: String): T = this?.singleOrNull()
    ?: throw PatchException("Expected one $name")
