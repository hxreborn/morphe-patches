/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.screenshot

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeScreenshotRestrictionPatch = patch(
    name = "Remove screenshot restriction",
    description = "Allows screenshots and screen recording on every screen.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        SecureWindowFlagFingerprint.match().apply {
            method.removeInstruction(instructionMatches.last().index)
        }
    }
}
