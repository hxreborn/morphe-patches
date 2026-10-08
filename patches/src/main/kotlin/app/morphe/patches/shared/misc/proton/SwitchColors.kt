/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import org.w3c.dom.Element

private const val SWITCH_COLORS_STYLE = "ThemeOverlay.Patches.MaterialSwitch"

private val SWITCH_COLOR_ROLES = mapOf(
    "colorPrimaryContainer" to "?attr/colorOnPrimary",
    "colorOnPrimaryContainer" to "?attr/colorPrimary",
    "colorOutline" to "?attr/proton_icon_hint",
    "colorOnSurfaceVariant" to "?attr/proton_icon_hint",
    "colorSurfaceVariant" to "?attr/proton_background_norm",
    "colorSurfaceContainerHighest" to "?attr/proton_background_norm",
)

internal val switchColorsPatch = resourcePatch {
    execute {
        val declaredAttributes = document("res/values/attrs.xml").use { document ->
            document.getElementsByTagName("attr").asSequence()
                .map { (it as Element).getAttribute("name") }
                .toSet()
        }

        document("res/values/styles.xml").use { document ->
            val style = document.createElement("style")
            style.setAttribute("name", SWITCH_COLORS_STYLE)
            style.setAttribute("parent", "")
            SWITCH_COLOR_ROLES.filterKeys { it in declaredAttributes }.forEach { (role, color) ->
                val item = document.createElement("item")
                item.setAttribute("name", role)
                item.textContent = color
                style.appendChild(item)
            }
            document.documentElement.appendChild(style)
        }
    }
}
