/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.graphmessenger.shared.licenseCheck
import app.morphe.patches.graphmessenger.shared.overrideResultsOf
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes banner, interstitial, native, app open and rewarded ads, " +
        "Graph's own promotions, Telegram sponsored messages and the proxy sponsor channel.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch, removeMobileAdsInitProviderPatch, removeAdNetworkProvidersPatch)

    execute {
        LicenseCheckFingerprint.matchAll().forEach { it.method.overrideResultsOf(licenseCheck, 1) }

        AdSdkInitFingerprint.matchSingle().method.returnEarly()
        SponsoredMessagesReadParamsFingerprint.matchSingle().method.returnEarly()
        SponsoredPeersReadParamsFingerprint.matchSingle().method.returnEarly()
        PromoDataRequestFingerprint.matchSingle().method.returnEarly()
    }
}
