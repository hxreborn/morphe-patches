/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.serializd.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.playwire.stopPlaywireSdkPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics and the Playwire advertising SDK from collecting " +
        "usage data and the advertising ID.",
) {
    compatibleWith(AppCompatibilities.SERIALIZD)

    dependsOn(disableAnalyticsCollectionPatch, stopPlaywireSdkPatch)
}
