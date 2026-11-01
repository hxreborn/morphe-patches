/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.neetprepessential.misc.screenshot

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeScreenshotRestrictionPatch = bytecodePatch(
    name = "Remove screenshot restriction",
    description = "Allows screenshots and screen recording on every screen.",
) {
    compatibleWith(AppCompatibilities.NEETPREP_ESSENTIAL)

    execute {
        ScreenProtectorSecureFlagFingerprint.matchSingle().apply {
            method.removeInstruction(instructionMatches.last().index)
        }
    }
}
