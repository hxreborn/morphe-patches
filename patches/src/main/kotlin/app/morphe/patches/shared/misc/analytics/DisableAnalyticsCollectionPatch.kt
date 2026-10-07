/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.analytics

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import org.w3c.dom.Document
import org.w3c.dom.Element

private val disabledCollectionFlags = listOf(
    "firebase_analytics_collection_enabled",
    "google_analytics_adid_collection_enabled",
    "google_analytics_ssaid_collection_enabled",
    "com.facebook.sdk.AutoLogAppEventsEnabled",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled",
)

internal fun Document.putApplicationMetaData(name: String, value: String) {
    val application = getElementsByTagName("application").item(0) as Element
    val declared = application.childNodes.asSequence().filterIsInstance<Element>().firstOrNull {
        it.tagName == "meta-data" && it.getAttribute("android:name") == name
    }
    val metadata = declared ?: createElement("meta-data").also {
        it.setAttribute("android:name", name)
        application.appendChild(it)
    }
    metadata.removeAttribute("android:resource")
    metadata.setAttribute("android:value", value)
}

val disableAnalyticsCollectionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            disabledCollectionFlags.forEach { document.putApplicationMetaData(it, "false") }
        }
    }
}

val disableCrashlyticsCollectionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_crashlytics_collection_enabled", "false")
        }
    }
}
