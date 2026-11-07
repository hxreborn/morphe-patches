/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.recommendations

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.VIDEO_RECOMMENDATIONS_HIDDEN
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.overrideWhenEnabled
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch

@Suppress("unused")
val hideVideoRecommendationsPatch: BytecodePatch = bytecodePatch(
    name = "Hide video recommendations",
    description = "Removes recommendations below the player.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("hideVideoRecommendations", hideVideoRecommendationsPatch)
        VideoRecommendationsFingerprint.matchAll(2..2).forEach { it.method.overrideWhenEnabled(VIDEO_RECOMMENDATIONS_HIDDEN) { returnEarly() } }
    }
}
