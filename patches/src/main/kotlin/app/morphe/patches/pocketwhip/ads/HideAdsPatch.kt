/*
 * Copyright (C) 2026 Bogat25
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pocketwhip.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val VIEW_GONE = 0x8

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides the banner and stops ads from loading.",
) {
    compatibleWith(AppCompatibilities.POCKET_WHIP)

    dependsOn(removeMobileAdsInitProviderPatch)

    execute {
        // Hide the banner instead of loading it. Patching the SDK rather than the app keeps the app's own
        // banner setup intact (it still holds and later uses the view) and covers any banner it shows.
        // p1 (the ad request) is free to reuse because the method returns right away.
        BannerLoadAdFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/16 p1, $VIEW_GONE
                invoke-virtual/range { p0 .. p1 }, Landroid/view/View;->setVisibility(I)V
                return-void
            """,
        )
        MobileAdsInitializeFingerprint.matchSingle().method.returnEarly()
        RewardedAdLoadFingerprint.matchSingle().method.returnEarly()
    }
}
