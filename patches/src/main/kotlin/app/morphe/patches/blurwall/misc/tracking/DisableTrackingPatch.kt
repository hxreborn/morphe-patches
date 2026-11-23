/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blurwall.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops the Google Mobile Ads SDK from starting and reading the advertising ID.",
) {
    compatibleWith(AppCompatibilities.BLURWALL)

    dependsOn(removePairipProtectionPatch, removeMobileAdsInitProviderPatch)
}
