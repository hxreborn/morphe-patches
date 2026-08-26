/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getFreeRegisterProvider

internal const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/photoeditorpro"
internal const val PATCH_GATES_CLASS = "$EXTENSION_PACKAGE/PatchGates;"
internal const val PATCH_PANEL_CLASS = "$EXTENSION_PACKAGE/PatchPanel;"
internal fun MutableMethod.returnTrueWhileEnabled(gateMethod: String) {
    val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $PATCH_GATES_CLASS->$gateMethod()Z
            move-result v$register
            if-eqz v$register, :stock
            const/4 v$register, 0x1
            return v$register
            :stock
            nop
        """,
    )
}

internal fun BytecodePatchContext.markPatchInstalled(settingKey: String) {
    mutableClassDefBy(PATCH_PANEL_CLASS)
        .methods.single { it.name == "<clinit>" }
        .apply {
            val end = implementation!!.instructions.count() - 1
            val register = getFreeRegisterProvider(end, 1).getFreeRegister4Bit()

            addInstructions(
                end,
                """
                    const-string v$register, "$settingKey"
                    invoke-static { v$register }, $PATCH_PANEL_CLASS->markInstalled(Ljava/lang/String;)V
                """,
            )
        }
}
