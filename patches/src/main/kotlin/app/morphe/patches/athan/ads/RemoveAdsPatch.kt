/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.BannerLoadAdFingerprint
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes banner, native, app open and interstitial ads, including the ad shown before " +
        "sharing.",
) {
    compatibleWith(AppCompatibilities.ATHAN)

    dependsOn(spoofSignaturePatch, failAdMobLoadsPatch, removeMobileAdsInitProviderPatch)

    execute {
        BannerLoadAdFingerprint.matchSingle().method.returnEarly()

        InterstitialBeforeActionFingerprint.matchSingle().method.addInstructionsWithLabels(
            0,
            """
                if-eqz p3, :done
                invoke-interface { p3 }, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
                :done
                return-void
            """,
        )
    }
}
