/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.recommendations

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.returnEarly

@Suppress("unused")
val hideVideoRecommendationsPatch = bytecodePatch(
    name = "Hide video recommendations",
    description = "Hides the recommended videos below the video player.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch)

    execute {
        VideoRecommendationsFingerprint.matchAll(2..2).forEach { it.method.returnEarly() }
    }
}
