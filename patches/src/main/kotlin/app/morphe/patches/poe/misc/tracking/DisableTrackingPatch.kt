/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.poe.misc.tracking

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.poe.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData

private val disableFirebaseSessionsAndPerformancePatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_sessions_enabled", "false")
            document.putApplicationMetaData("firebase_performance_collection_enabled", "false")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Sessions and Performance Monitoring " +
        "from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.POE)

    dependsOn(
        spoofSignaturePatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsAndPerformancePatch,
    )
}
