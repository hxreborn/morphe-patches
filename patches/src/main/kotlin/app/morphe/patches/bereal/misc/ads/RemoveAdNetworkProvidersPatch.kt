/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import org.w3c.dom.Element

private val AD_NETWORK_PROVIDERS = listOf(
    "com.applovin.sdk.AppLovinInitProvider",
    "com.google.android.gms.ads.MobileAdsInitProvider",
    "com.inmobi.sdk.InMobiInitProvider",
)

internal val removeAdNetworkProvidersPatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider").asSequence().filterIsInstance<Element>().toList()
            AD_NETWORK_PROVIDERS.forEach { name ->
                providers.singleOrNull { it.getAttribute("android:name") == name }?.removeFromParent()
                    ?: throw PatchException("Expected exactly one $name in the manifest")
                mutableClassDefBy("L${name.replace('.', '/')};").methods
                    .single { it.name == "onCreate" && it.parameterTypes.isEmpty() }
                    .returnEarly(true)
            }
        }
    }
}
