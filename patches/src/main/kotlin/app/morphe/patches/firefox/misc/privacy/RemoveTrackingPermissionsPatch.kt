/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.privacy

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import org.w3c.dom.Element

private val trackingPermissions = setOf(
    "com.google.android.gms.permission.AD_ID",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

@Suppress("unused")
val removeTrackingPermissionsPatch = resourcePatch(
    name = "Remove ad ID and referrer permissions",
    description = "Removes the advertising ID and install referrer permissions.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val declared = manifest.childNodes.asSequence().filterIsInstance<Element>()
                .filter { it.tagName == "uses-permission" && it.getAttribute("android:name") in trackingPermissions }
                .toList()
            val missing = trackingPermissions - declared.map { it.getAttribute("android:name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Permissions not declared: $missing")
            declared.forEach { manifest.removeChild(it) }
        }
    }
}
