/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.tracking

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private val disableCrashlyticsCollectionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_crashlytics_collection_enabled", "false")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = resourcePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics and Crashlytics from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.KEEPA)

    dependsOn(
        removePairipProtectionPatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
    )
}
