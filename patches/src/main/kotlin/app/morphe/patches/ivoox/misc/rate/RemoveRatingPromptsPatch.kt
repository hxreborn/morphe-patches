/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ivoox.misc.rate

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeRatingPromptsPatch = patch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.IVOOX)

    execute {
        ShowRateDialogPreferenceFingerprint.matchSingle().method.returnEarly(false)
    }
}
