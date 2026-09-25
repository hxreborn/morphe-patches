/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.analytics

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private val disabledCollectionFlags = listOf(
    "firebase_analytics_collection_enabled",
    "google_analytics_adid_collection_enabled",
    "google_analytics_ssaid_collection_enabled",
    "com.facebook.sdk.AutoLogAppEventsEnabled",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled",
)

val disableAnalyticsCollectionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)
            val declared = buildSet {
                val existing = document.getElementsByTagName("meta-data")
                for (index in 0 until existing.length) {
                    add((existing.item(index) as Element).getAttribute("android:name"))
                }
            }

            disabledCollectionFlags.forEach { name ->
                if (name in declared) return@forEach
                val metadata = document.createElement("meta-data")
                metadata.setAttribute("android:name", name)
                metadata.setAttribute("android:value", "false")
                application.appendChild(metadata)
            }
        }
    }
}
