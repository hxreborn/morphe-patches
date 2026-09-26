/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch

private const val OVERLAY_STYLE = "ThemeOverlay.Patches.AmoledBackground"
private const val BACKGROUND_ATTRIBUTE = "proton_background_norm"
private const val AMOLED_BACKGROUND_OVERLAY_CLASS = "${PROTON_EXTENSION_PACKAGE}AmoledBackgroundOverlay;"

private val amoledBackgroundOverlayStylePatch = resourcePatch {
    execute {
        document("res/values/styles.xml").use { document ->
            val style = document.createElement("style")
            style.setAttribute("name", OVERLAY_STYLE)

            val item = document.createElement("item")
            item.setAttribute("name", BACKGROUND_ATTRIBUTE)
            item.textContent = "@android:color/black"
            style.appendChild(item)

            document.documentElement.appendChild(style)
        }
    }
}

internal val amoledBackgroundOverlayPatch = bytecodePatch {
    dependsOn(amoledBackgroundOverlayStylePatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(AMOLED_BACKGROUND_OVERLAY_CLASS)
    }
}
