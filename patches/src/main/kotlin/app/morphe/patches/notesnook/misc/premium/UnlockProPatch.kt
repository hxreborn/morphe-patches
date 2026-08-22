/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notesnook.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/notesnook/ProUnlock;"

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock pro",
    description = "Unlocks task lists, callouts, app lock, and the notebook, tag, colour and " +
        "reminder limits. Requires a signed-in account. The server still enforces storage, " +
        "attachment size, monographs and SMS 2FA.",
) {
    compatibleWith(AppCompatibilities.NOTESNOOK)
    extendWith("extensions/extension.mpe")

    execute {
        ResponseHandlerFingerprint.matchSingle().apply {
            val index = instructionMatches[0].index
            val register = method.getInstruction<FiveRegisterInstruction>(index).registerD

            method.addInstructions(
                index,
                "invoke-static { v$register }, $EXTENSION_CLASS->rewriteSubscription([B)[B\n" +
                    "move-result-object v$register",
            )
        }
    }
}
