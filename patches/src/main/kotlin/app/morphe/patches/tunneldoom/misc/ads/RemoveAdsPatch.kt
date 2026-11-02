/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tunneldoom.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.BannerLoadAdFingerprint
import app.morphe.patches.shared.misc.ads.ConsentInfoUpdateFingerprint
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val REPORT_LOAD_FAILURE =
    "Lapp/hxreborn/extension/shared/AdMobLoadFailure;->report(Ljava/lang/Object;)V"

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes app open, banner, interstitial and rewarded ads and the ad consent form.",
) {
    compatibleWith(AppCompatibilities.TUNNELDOOM_VPN)

    dependsOn(removePairipProtectionPatch, failAdMobLoadsPatch, removeMobileAdsInitProviderPatch)

    execute {
        (RewardedAdLoadFingerprint.matchAll() + RewardedInterstitialAdLoadFingerprint.matchAll()).forEach { match ->
            match.method.addInstructions(
                0,
                "invoke-static { p${match.method.parameters.size - 1} }, $REPORT_LOAD_FAILURE\n" +
                    "return-void",
            )
        }

        BannerLoadAdFingerprint.matchSingle().method.returnEarly()
        ConsentInfoUpdateFingerprint.matchSingle().method.returnEarly()
    }
}
