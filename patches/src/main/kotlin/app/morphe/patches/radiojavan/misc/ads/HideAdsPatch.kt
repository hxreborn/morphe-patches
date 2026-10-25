/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.radiojavan.misc.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.radiojavan.shared.subscriptionGetterFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch

internal val disableSubscriptionAdFlagsPatch = bytecodePatch {
    execute {
        listOf("getDisplayAds", "getAudioAds").forEach {
            subscriptionGetterFingerprint(it).matchSingle().method.returnEarly(false)
        }
    }
}

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes banner, interstitial, app open and podcast audio ads, the launch promo video " +
        "and the ad consent form.",
) {
    compatibleWith(AppCompatibilities.RADIO_JAVAN)

    dependsOn(disableSubscriptionAdFlagsPatch, failAdMobLoadsPatch, removeMobileAdsInitProviderPatch)

    execute {
        SetCtaOverlayFingerprint.matchSingle().method.returnEarly()
        ConsentRequestFingerprint.matchSingle().method.returnEarly()
        MobileAdsInitializeFingerprint.matchSingle().method.returnEarly()
    }
}
