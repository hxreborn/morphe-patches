/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.arcplayer.misc.rate

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeRatingPromptsPatch = patch(
    name = "Remove rating prompts",
    description = "Removes the feedback and rating popup that appears after watching videos.",
) {
    compatibleWith(AppCompatibilities.ARC_PLAYER)

    execute {
        ShowRatingDialogFingerprint.method.returnEarly()
    }
}
