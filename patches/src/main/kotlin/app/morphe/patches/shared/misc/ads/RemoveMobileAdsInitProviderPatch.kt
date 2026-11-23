/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import org.w3c.dom.Element

private const val MOBILE_ADS_INIT_PROVIDER = "com.google.android.gms.ads.MobileAdsInitProvider"

val removeMobileAdsInitProviderPatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider")
            val initProvider = providers.asSequence()
                .filterIsInstance<Element>()
                .singleOrNull { it.getAttribute("android:name") == MOBILE_ADS_INIT_PROVIDER }
                ?: throw PatchException("Expected exactly one $MOBILE_ADS_INIT_PROVIDER in the manifest")
            initProvider.removeFromParent()
        }
    }
}
