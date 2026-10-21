/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import org.w3c.dom.Element

private val AD_NETWORK_PROVIDERS = listOf(
    "com.applovin.sdk.AppLovinInitProvider",
    "com.facebook.ads.AudienceNetworkContentProvider",
)

internal val removeAdNetworkProvidersPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider").asSequence().filterIsInstance<Element>().toList()
            AD_NETWORK_PROVIDERS.forEach { name ->
                providers.singleOrNull { it.getAttribute("android:name") == name }?.removeFromParent()
                    ?: throw PatchException("Expected exactly one $name in the manifest")
            }
        }
    }
}
