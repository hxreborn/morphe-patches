/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.rate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val removeRatingPromptsPatch = patch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.RINGTONE_MAKER)

    execute {
        val dialog = RatingDialogFingerprint.matchSingle()
        val (_, _, result) = dialog.instructionMatches
        val register = (result.instruction as OneRegisterInstruction).registerA
        dialog.method.addInstruction(result.index + 1, "const/4 v$register, 0x1")
    }
}
