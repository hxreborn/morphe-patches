/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iiec.misc.fix.signature

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

internal val removeSharedUserIdPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            manifest.removeAttribute("android:sharedUserId")
            manifest.removeAttribute("android:sharedUserLabel")
        }
    }
}
