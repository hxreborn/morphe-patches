/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.rate

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val removeRatingPromptsPatch = bytecodePatch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.RINGTONE_MAKER)

    execute {
        ShowRatingDialogFingerprint.matchSingle().method.returnEarly()
    }
}
