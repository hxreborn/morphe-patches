/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes splash, banner, native and interstitial ads, including the ad shown when a " +
        "cloned app opens, and the ad consent form.",
) {
    compatibleWith(AppCompatibilities.CLONE_APP)

    dependsOn(failAdMobLoadsPatch, removeMobileAdsInitProviderPatch)

    execute {
        SplashAdEligibilityFingerprint.matchSingle().method.returnEarly(false)
        OtherAdEligibilityFingerprint.matchSingle().method.returnEarly(false)
        AdConsentAndInitFingerprint.matchSingle().method.returnEarly()
        EmbeddedAdsFlagFingerprint.matchAll(2..2).forEach { it.method.returnEarly(false) }
    }
}
