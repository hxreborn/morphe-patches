/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gammascan.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Disables banner, interstitial, and native ads.",
) {
    compatibleWith(AppCompatibilities.GAMMA_SCAN)

    execute {
        BannerAdLoadFingerprint.matchOrNull()?.method?.returnEarly()
        InterstitialAdLoadFingerprint.matchOrNull()?.method?.returnEarly()
        NativeAdLoadFingerprint.matchSingle().method.returnEarly()
    }
}
