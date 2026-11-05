/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel

internal const val VIBER_SETTINGS_CLASS = "Lapp/hxreborn/extension/viber/PatchSettings;"

internal fun BytecodePatchContext.markPatchApplied(name: String, description: String) {
    mutableClassDefBy("Lapp/hxreborn/extension/viber/AppliedPatches;")
        .methods.single { it.name == "${name}Description" }.returnEarly(description)
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
