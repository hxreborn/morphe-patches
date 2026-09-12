/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.skyla

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val hideSkylaPatch = bytecodePatch(
    name = "Hide Skyla",
    description = "Hides the Skyla AI assistant, its prompts and the Summarize buttons.",
) {
    compatibleWith(AppCompatibilities.ONE_WEATHER)

    execute {
        SkylaFeatureEnabledFingerprint.matchSingle().method.returnEarly(false)
    }
}
