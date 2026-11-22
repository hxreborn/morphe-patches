/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notesnook.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patches.shared.misc.extension.notesnookUnlockProExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/notesnook/ProUnlock;"

@Suppress("unused")
val unlockProPatch = patch(
    name = "Unlock pro",
    description = "Unlocks task lists, callouts, app lock, and the notebook, tag, colour and " +
        "reminder limits. Requires a signed-in account. The server still enforces storage, " +
        "attachment size, monographs and SMS 2FA.",
) {
    dependsOn(notesnookUnlockProExtensionPatch)
    compatibleWith(AppCompatibilities.NOTESNOOK)
    execute {
        ResponseHandlerFingerprint.match().apply {
            val (toResponseData) = instructionMatches
            val register = toResponseData.getInstruction<FiveRegisterInstruction>().registerD

            method.addInstructions(
                toResponseData.index,
                "invoke-static { v$register }, $EXTENSION_CLASS->rewriteSubscription([B)[B\n" +
                    "move-result-object v$register",
            )
        }
    }
}
