/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.googleaccount

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BROWSE_ACCOUNT_SETTINGS_URI =
    "intent:#Intent;action=com.google.android.gms.accountsettings.action.BROWSE_SETTINGS;i.extra.screenId=1;end"

@Suppress("unused")
val fixManageGoogleAccountPatch = bytecodePatch(
    name = "Fix Manage Google account",
    description = "Opens the Google Account settings from Manage Google account.",
) {
    compatibleWith(AppCompatibilities.CLONE_APP)

    execute {
        AccountSettingsIntentFingerprint.matchSingle().apply {
            val (newIntentIndex, actionIndex, constructorIndex) = instructionMatches.map { it.index }
            val intentRegister = method.getInstruction<FiveRegisterInstruction>(constructorIndex).registerC
            val uriRegister = method.getInstruction<OneRegisterInstruction>(actionIndex).registerA

            method.replaceInstruction(
                constructorIndex,
                "invoke-static { v$uriRegister }, " +
                    "Landroid/content/Intent;->getIntent(Ljava/lang/String;)Landroid/content/Intent;",
            )
            method.addInstruction(constructorIndex + 1, "move-result-object v$intentRegister")
            method.replaceInstruction(actionIndex, "const-string v$uriRegister, \"$BROWSE_ACCOUNT_SETTINGS_URI\"")
            method.removeInstruction(newIntentIndex)
        }
    }
}
