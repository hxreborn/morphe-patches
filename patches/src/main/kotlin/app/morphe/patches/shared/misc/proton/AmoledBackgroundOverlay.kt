/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedNightModeExtensionPatch

internal const val AMOLED_BACKGROUND_OVERLAY_STYLE = "ThemeOverlay.Patches.AmoledBackground"
private const val BACKGROUND_ATTRIBUTE = "proton_background_norm"
private const val AMOLED_BACKGROUND_OVERLAY_CLASS = "${PROTON_EXTENSION_PACKAGE}AmoledBackgroundOverlay;"

private val amoledBackgroundOverlayStylePatch = patch {
    execute {
        document("res/values/styles.xml").use { document ->
            val style = document.createElement("style")
            style.setAttribute("name", AMOLED_BACKGROUND_OVERLAY_STYLE)

            val item = document.createElement("item")
            item.setAttribute("name", BACKGROUND_ATTRIBUTE)
            item.textContent = "@android:color/black"
            style.appendChild(item)

            document.documentElement.appendChild(style)
        }
    }
}

internal val amoledBackgroundOverlayPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedNightModeExtensionPatch)
    dependsOn(amoledBackgroundOverlayStylePatch)

    execute {
        markFeaturePatched(AMOLED_BACKGROUND_OVERLAY_CLASS)
    }
}
