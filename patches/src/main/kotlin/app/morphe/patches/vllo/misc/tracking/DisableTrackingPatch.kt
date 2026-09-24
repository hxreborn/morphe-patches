/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.vllo.requireArm64Delta
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import org.w3c.dom.Element

private val disabledCollectionFlags = listOf(
    "firebase_analytics_collection_enabled",
    "google_analytics_adid_collection_enabled",
    "google_analytics_ssaid_collection_enabled",
    "com.facebook.sdk.AutoLogAppEventsEnabled",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled",
)

private val disableAnalyticsCollectionPatch = resourcePatch {
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

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Firebase Analytics, and Facebook from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.VLLO)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        disableAnalyticsCollectionPatch,
    )

    availability(requireArm64Delta)

    execute {
        AppsFlyerStartFingerprint.matchSingle().method.returnEarly()
    }
}
