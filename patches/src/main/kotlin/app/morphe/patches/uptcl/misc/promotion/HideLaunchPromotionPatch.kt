/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.promotion

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.uptcl.misc.fix.signature.trustInstalledCertificatePatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val hideLaunchPromotionPatch = bytecodePatch(
    name = "Hide launch promotion",
    description = "Hides the promotional banner and video on the launch splash screen and the full-screen offer " +
        "on the home screen.",
) {
    compatibleWith(AppCompatibilities.UPTCL)
    dependsOn(trustInstalledCertificatePatch)

    execute {
        listOf(ShowLaunchSplashFingerprint, ShowLaunchBannerFingerprint, ShowLaunchVideoFingerprint).forEach {
            it.matchSingle().method.returnEarly("false")
        }
        ShowFullScreenOfferFingerprint.matchSingle().method.returnEarly()
    }
}
