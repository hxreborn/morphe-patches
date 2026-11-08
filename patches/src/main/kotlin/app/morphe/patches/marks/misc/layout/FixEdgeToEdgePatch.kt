/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.layout

import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val fixEdgeToEdgePatch: ResourcePatch = resourcePatch(
    name = "Fix edge-to-edge",
    description = "Keeps the medical interface clear of system bars.",
) {
    compatibleWith(AppCompatibilities.MARKS)

    execute {
        document("res/layout/activity_medical.xml").use { document ->
            document.documentElement.setAttribute("android:fitsSystemWindows", "true")
        }
    }
}
