/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.equalizer

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import app.morphe.patches.ultimea.misc.settings.NativeFunction
import app.morphe.patches.ultimea.misc.settings.nativeMethods

private const val NOP = 0xd503201f.toInt()
private const val INSTRUCTION_BYTES = 4
private const val REGISTER_MASK = 31
private const val BASE_REGISTER_SHIFT = 5
private const val MOVE_SOURCE_SHIFT = 16
private const val FRAME_POINTER = 29
private const val DEVICE_CURVE_COPY_WINDOW_BYTES = 48
private const val CONTROLLER_LOAD_WINDOW_BYTES = 16
private const val UNSCALED_LOAD_MASK = 0xffe00c00.toInt()
private const val LOAD_COMPRESSED_FIELD = 0xb8400000.toInt()
private const val LOAD_LONG = 0xf8400000.toInt()
private const val STACK_ACCESS_MASK = 0xffe00fff.toInt()
private const val STACK_SLOT_MASK = 0x001ff000
private const val STORE_STACK = 0xf80003a0.toInt()
private const val LOAD_STACK_RECEIVER = 0xf84003a1.toInt()
private const val LOAD_STACK_ARGUMENT = 0xf84003a2.toInt()
private const val DECOMPRESS_POINTER = 0x8b1c8000.toInt()
private const val COPY_RETURN_TO_ARGUMENT = 0xaa0003e2.toInt()
private const val MOVE_REGISTER = 0xaa0003e0.toInt()
private const val RECEIVER_REGISTER = 1
private const val BRANCH = 0x14000000
private const val BRANCH_MASK = 0x03ffffff

private data class DeviceCurveAssignment(val start: Int, val controller: Int, val setter: Int)

internal fun ByteArray.stopAutomaticPresetApply() {
    val refresh = nativeMethods.single("_requestUserCustomEqData2 = <")
    val savedPresetSetters = nativeMethods.matching("applyServerCustomEq = <userCustomEqDataModels is empty>")
        .map { it.start }.toSet()
    val apply = nativeMethods.calls(refresh).singleOrNull {
        nativeMethods.callTarget(it) in savedPresetSetters
    } ?: throw PatchException("Expected one automatic saved equalizer preset application")
    if (readInt(apply) != nativeMethods.bytes.readInt(apply)) {
        throw PatchException("Automatic equalizer preset application was already modified")
    }
    val marker = refresh.loads("_requestUserCustomEqData2 = <").single()
    val applyMethod = nativeMethods.containing(nativeMethods.callTarget(apply))
    val applyTargets = nativeMethods.calls(applyMethod).map(nativeMethods::callTarget).toSet()
    val assignment = deviceCurveAssignment(refresh, marker, applyTargets)
    val display = nativeMethods.calls(refresh).singleOrNull { call ->
        if (call <= marker || call >= apply) return@singleOrNull false
        val target = nativeMethods.callTarget(call)
        val function = nativeMethods.containing(target)
        function.start == target && function.stringLoads.isEmpty() && nativeMethods.calls(function).any {
            nativeMethods.callTarget(it) == assignment.setter
        }
    } ?: throw PatchException("Expected one automatic saved equalizer display assignment")
    val controllerLoad = readInt(display - 2 * INSTRUCTION_BYTES)
    if (controllerLoad and STACK_ACCESS_MASK != LOAD_STACK_RECEIVER ||
        readInt(display - INSTRUCTION_BYTES) and STACK_ACCESS_MASK != LOAD_STACK_ARGUMENT) {
        throw PatchException("Expected saved equalizer display arguments")
    }
    val controllerSlot = controllerLoad and STACK_SLOT_MASK
    (maxOf(refresh.start, assignment.start - CONTROLLER_LOAD_WINDOW_BYTES) until assignment.start step INSTRUCTION_BYTES)
        .singleOrNull { address ->
            val instruction = readInt(address)
            instruction and UNSCALED_LOAD_MASK == LOAD_LONG &&
                (instruction ushr BASE_REGISTER_SHIFT) and REGISTER_MASK == FRAME_POINTER &&
                instruction and REGISTER_MASK == assignment.controller && instruction and STACK_SLOT_MASK == controllerSlot
        } ?: throw PatchException("Expected shared equalizer controller for device and saved curves")
    if (readInt(display) != nativeMethods.bytes.readInt(display) ||
        readInt(display - INSTRUCTION_BYTES) != nativeMethods.bytes.readInt(display - INSTRUCTION_BYTES)) {
        throw PatchException("Automatic equalizer display assignment was already modified")
    }
    writeInt(display - INSTRUCTION_BYTES,
        MOVE_REGISTER or (RECEIVER_REGISTER shl MOVE_SOURCE_SHIFT) or assignment.controller)
    writeInt(display, BRANCH or (((assignment.start - display) / INSTRUCTION_BYTES) and BRANCH_MASK))
    writeInt(apply, NOP)
}

private fun ByteArray.deviceCurveAssignment(refresh: NativeFunction, marker: Int, applyTargets: Set<Int>): DeviceCurveAssignment {
    val calls = nativeMethods.calls(refresh)
    return (refresh.start until marker step INSTRUCTION_BYTES).mapNotNull { address ->
        val load = readInt(address)
        val register = load and REGISTER_MASK
        val controller = (load ushr BASE_REGISTER_SHIFT) and REGISTER_MASK
        val store = readInt(address + 2 * INSTRUCTION_BYTES)
        if (load and UNSCALED_LOAD_MASK != LOAD_COMPRESSED_FIELD || controller == FRAME_POINTER ||
            readInt(address + INSTRUCTION_BYTES) != (DECOMPRESS_POINTER or (register shl BASE_REGISTER_SHIFT) or register) ||
            store and STACK_ACCESS_MASK != (STORE_STACK or register)) return@mapNotNull null
        val getter = calls.singleOrNull { call ->
            call >= address + 3 * INSTRUCTION_BYTES && call < address + DEVICE_CURVE_COPY_WINDOW_BYTES &&
                nativeMethods.callTarget(call) in applyTargets &&
                readInt(call + INSTRUCTION_BYTES) == (LOAD_STACK_RECEIVER or (store and STACK_SLOT_MASK)) &&
                readInt(call + 2 * INSTRUCTION_BYTES) == COPY_RETURN_TO_ARGUMENT &&
                call + 3 * INSTRUCTION_BYTES in calls
        } ?: return@mapNotNull null
        DeviceCurveAssignment(address, controller, nativeMethods.callTarget(getter + 3 * INSTRUCTION_BYTES))
    }.singleOrNull() ?: throw PatchException("Expected one device equalizer display assignment")
}
