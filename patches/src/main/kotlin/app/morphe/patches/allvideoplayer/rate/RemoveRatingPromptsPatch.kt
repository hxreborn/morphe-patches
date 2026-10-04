/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.rate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val removeRatingPromptsPatch = bytecodePatch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)

    execute {
        RatingPromptGateFingerprint.matchSingle().apply {
            val gate = instructionMatches.last()
            val shownCount = gate.getInstruction<TwoRegisterInstruction>().registerA
            method.addInstruction(gate.index, "const/4 v$shownCount, 0x3")
        }
    }
}
