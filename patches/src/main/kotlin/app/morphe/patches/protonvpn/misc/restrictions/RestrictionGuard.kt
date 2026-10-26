/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/splittunneling/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val LOCAL_USER_SETTINGS = "Lcom/protonvpn/android/settings/data/LocalUserSettings;"

internal fun BytecodePatchContext.restrictionGuardFingerprint(settingGetter: String): Fingerprint {
    val vpnUser = vpnUserType
    return Fingerprint(
        returnType = LOCAL_USER_SETTINGS,
        filters = listOf(
            opcode(Opcode.IF_EQZ),
            methodCall(
                definingClass = LOCAL_USER_SETTINGS,
                name = settingGetter,
                location = MatchAfterImmediately(),
            ),
        ),
        custom = { method, _ ->
            method.parameterTypes.firstOrNull()?.toString() == LOCAL_USER_SETTINGS &&
                method.parameterTypes.any { it.toString() == vpnUser }
        },
    )
}

private fun Instruction.argumentRegisters() = when (this) {
    is RegisterRangeInstruction -> List(registerCount) { startRegister + it }
    is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
    else -> throw PatchException("$opcode is not an invoke")
}

private fun MethodReference.argumentSlotTypes() = (listOf(definingClass) + parameterTypes.map { it.toString() })
    .flatMap { if (it == "J" || it == "D") listOf(it, it) else listOf(it) }

internal fun BytecodePatchContext.unlockUserSetting(viewState: Fingerprint, restriction: Fingerprint) {
    viewState.matchSingle().run {
        val title = instructionMatches.first()
        val titleRegister = title.getInstruction<OneRegisterInstruction>().registerA
        val superclass = classDef.superclass
        val superCallIndex = method.indexOfFirstInstructionOrThrow(title.index) {
            (opcode == Opcode.INVOKE_DIRECT || opcode == Opcode.INVOKE_DIRECT_RANGE) &&
                getReference<MethodReference>()?.let { it.name == "<init>" && it.definingClass == superclass } == true
        }
        val superCall = method.getInstruction(superCallIndex)
        val arguments = superCall.argumentRegisters()
        val slotTypes = superCall.getReference<MethodReference>()!!.argumentSlotTypes()
        val titleSlot = arguments.indexOf(titleRegister)
        if (titleSlot < 1 || slotTypes[titleSlot] != "I" || slotTypes[titleSlot - 1] != "Z") {
            throw PatchException("No restricted flag beside the title resource in the super constructor call of $method")
        }
        method.addInstruction(superCallIndex, "const/16 v${arguments[titleSlot - 1]}, 0x0")
    }
    keepUserSetting(restriction)
}

internal fun BytecodePatchContext.keepUserSetting(restriction: Fingerprint) {
    restriction.matchSingle().run {
        method.replaceInstruction(instructionMatches.first().index, "nop")
    }
}
