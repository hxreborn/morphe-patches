/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moonreader.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.removeMobileAdsInitProviderPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics from collecting usage data " +
        "and the Google Mobile Ads SDK from starting at launch.",
) {
    compatibleWith(AppCompatibilities.MOON_READER)

    dependsOn(disableAnalyticsCollectionPatch, removeMobileAdsInitProviderPatch)
}
