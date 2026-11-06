/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.util.StringUtils.escapeString

internal const val VIBER_SETTINGS_CLASS = "Lapp/hxreborn/extension/viber/PatchSettings;"

internal fun BytecodePatchContext.markPatchApplied(methodName: String, patch: BytecodePatch) {
    val methods = mutableClassDefBy("Lapp/hxreborn/extension/viber/AppliedPatches;").methods
    methods.single { it.name == methodName }.returnEarly(true)
    methods.single { it.name == methodName + "Name" }.returnEarly(escapeString(checkNotNull(patch.name)))
    methods.single { it.name == methodName + "Description" }.returnEarly(escapeString(checkNotNull(patch.description)))
}

context(_: BytecodePatchContext)
internal fun MutableMethod.returnWhenEnabled(settingCall: String, result: ((Int) -> String)? = null) {
    val register = implementation!!.registerCount
    val (expanded, parameterMoves) = addFreeRegisters(1)
    val returned = result?.invoke(register) ?: when (returnType) {
        "V" -> "return-void"
        "Ljava/lang/Boolean;" -> "sget-object v$register, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;\nreturn-object v$register"
        "Z" -> "const/16 v$register, 0x0\nreturn v$register"
        else -> throw PatchException("Unexpected guarded return type: $returnType")
    }
    expanded.addInstructionsWithLabels(
        parameterMoves,
        """
            invoke-static {}, $settingCall
            move-result v$register
            if-eqz v$register, :original
            $returned
        """,
        ExternalLabel("original", expanded.getInstruction(parameterMoves)),
    )
}

internal fun MutableMethod.addFilterCall(index: Int, register: String, call: String) {
    val returnType = call.substringAfterLast(')')
    val wide = returnType == "J" || returnType == "D"
    val lastRegister = if (wide) register.first() + (register.drop(1).toInt() + 1).toString() else register
    val moveResult = when {
        wide -> "move-result-wide"
        returnType.length == 1 -> "move-result"
        else -> "move-result-object"
    }
    addInstructions(
        index,
        """
            invoke-static/range { $register .. $lastRegister }, $call
            $moveResult $register
        """,
    )
}

internal fun MutableMethod.addFilterCallAfter(index: Int, call: String) =
    addFilterCall(index + 1, "v${getInstruction<OneRegisterInstruction>(index).registerA}", call)
