/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.review

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.vpnify.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val disableRatingPromptPatch = bytecodePatch(
    name = "Disable rating prompt",
    description = "Stops the Google Play rating prompt from appearing.",
) {
    compatibleWith(AppCompatibilities.VPNIFY)
    dependsOn(spoofSignaturePatch)

    execute {
        RequestReviewFingerprint.matchSingle().method.returnEarly()
    }
}
