/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.tracking

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData

@Suppress("unused")
val disableTrackingPatch = resourcePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Firebase Sessions and Survicate surveys from " +
        "collecting usage data.",
) {
    compatibleWith(AppCompatibilities.MYSCRIPT_NOTES)

    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_data_collection_default_enabled", "false")
            document.putApplicationMetaData("firebase_sessions_enabled", "false")
        }
    }
}
