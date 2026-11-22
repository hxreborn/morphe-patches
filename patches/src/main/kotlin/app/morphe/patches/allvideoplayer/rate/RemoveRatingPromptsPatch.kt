/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.rate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val RATING_PROMPT_LIMIT = 3
private const val IGNORE_HOME_LAUNCH_COUNT = -1

@Suppress("unused")
val removeRatingPromptsPatch = patch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)

    execute {
        RatingPromptGateFingerprint.match().apply {
            val (_, _, _, shownCountComparison) = instructionMatches
            val shownCount = shownCountComparison.getInstruction<TwoRegisterInstruction>().registerA
            method.addInstruction(shownCountComparison.index, "const/4 v$shownCount, $RATING_PROMPT_LIMIT")
        }
        HomeRatingPromptGateFingerprint.match().apply {
            val (promptLaunchCount) = instructionMatches
            val register = promptLaunchCount.getInstruction<OneRegisterInstruction>().registerA
            method.addInstruction(promptLaunchCount.index + 1, "const/4 v$register, $IGNORE_HOME_LAUNCH_COUNT")
        }
    }
}
