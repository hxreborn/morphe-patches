/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import org.w3c.dom.Element

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED dark theme",
    description = "Makes the torrent and media page backgrounds pure black in dark mode.",
) {
    compatibleWith(AppCompatibilities.UTORRENT)
    availability(requireArm64)
    dependsOn(removeProtectionPatch)

    execute {
        document("res/drawable/bg_torrentlist_dark.xml").use { document ->
            val fills = document.getElementsByTagName("solid")
            if (fills.length != 1) throw PatchException("Expected one dark torrent page fill")
            val fill = fills.item(0) as Element
            if (fill.getAttribute("android:color") != "@color/color_dialog_bg_dark") {
                throw PatchException("Unexpected dark torrent page background")
            }
            fill.setAttribute("android:color", "#ff000000")
        }
    }
}
