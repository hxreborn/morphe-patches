/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gammascan.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Disables banner, interstitial, and native ads.",
) {
    compatibleWith(AppCompatibilities.GAMMA_SCAN)

    execute {
        BannerAdLoadFingerprint.matchSingle().method.returnEarly()
        InterstitialAdLoadFingerprint.matchSingle().method.returnEarly()
        NativeAdLoadFingerprint.matchSingle().method.returnEarly()
    }
}
