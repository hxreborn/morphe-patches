/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audible.startup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val LIBRARY_SWITCH_INDEX = 1

@Suppress("unused")
val openLibraryOnLaunchPatch = bytecodePatch(
    name = "Open Library on launch",
    description = "Opens the Library tab instead of Home on launch. Applies only while signed in.",
) {
    compatibleWith(AppCompatibilities.AUDIBLE)

    execute {
        val match = NextActivityRouterFingerprint.matchSingle()
        val matches = match.instructionMatches

        // Lowercased copy is Conflict at the merge
        val actionRegister = matches[0].getInstruction<FiveRegisterInstruction>().registerC
        val unmatchedActionIndex = matches[2].index
        val switchRegister = matches[2].getInstruction<TwoRegisterInstruction>().registerA
        val scratchRegister = matches[3].getInstruction<OneRegisterInstruction>().registerA

        match.method.addInstructionsWithLabels(
            unmatchedActionIndex + 1,
            """
                const-string v$scratchRegister, "android.intent.action.MAIN"
                invoke-virtual { v$actionRegister, v$scratchRegister }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v$scratchRegister
                if-eqz v$scratchRegister, :keep_default_destination
                const/4 v$switchRegister, $LIBRARY_SWITCH_INDEX
                :keep_default_destination
                nop
            """,
        )
    }
}
