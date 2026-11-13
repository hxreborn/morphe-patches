/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Braze and Firebase Analytics data collection and blocks reads of the " +
        "advertising ID and install referrer. Braze push and in-app messages stop too.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch)

    execute {
        TrackingEntryFingerprints.forEach { it.matchSingle().method.returnEarly() }
        BrazeModuleVoidEntryFingerprints.forEach { it.matchSingle().method.returnEarly() }
        BrazeIdFromFeatureFlagFingerprint.matchSingle().method.returnEarly("")
    }
}
