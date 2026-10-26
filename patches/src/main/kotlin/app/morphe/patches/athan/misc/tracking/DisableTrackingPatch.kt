/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData

private val disablePerformanceMonitoringPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_performance_collection_deactivated", "true")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops usage events from reaching Firebase and Facebook, and disables Crashlytics and " +
        "Firebase performance monitoring.",
) {
    compatibleWith(AppCompatibilities.ATHAN)

    dependsOn(
        disableAnalyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
        disablePerformanceMonitoringPatch,
        spoofSignaturePatch,
    )

    execute {
        AnalyticsTrackerFingerprint.matchAll().forEach { it.method.returnEarly() }
        FacebookActivateAppFingerprint.matchSingle().method.returnEarly()
    }
}
