/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.showly.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.childElementsSequence
import app.morphe.util.forEachChildElement
import org.w3c.dom.Element

private const val BLACK = "#000000"

private val COLOR_RESOURCE_NAMES = setOf(
    "colorBackground",
    "colorPrimary",
    "colorPrimaryDark",
)

private val APP_THEME_ITEM_NAMES = setOf(
    "colorCardBackground",
    "colorPlaceholderBackground",
)

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.SHOWLY)

    execute {
        var matched = 0

        document("res/values/colors.xml").use { document ->
            val resources = document.getElementsByTagName("resources").item(0) as Element
            resources.childElementsSequence().forEach { node ->
                if (node.getAttribute("name") in COLOR_RESOURCE_NAMES) {
                    node.textContent = BLACK
                    matched++
                }
            }
        }

        document("res/values/styles.xml").use { document ->
            val resources = document.getElementsByTagName("resources").item(0) as Element
            resources.childElementsSequence().forEach { style ->
                if (style.nodeName != "style" || style.getAttribute("name") != "AppTheme") return@forEach

                style.forEachChildElement { item ->
                    if (item.getAttribute("name") in APP_THEME_ITEM_NAMES) {
                        item.textContent = BLACK
                        matched++
                    }
                }
            }
        }

        if (matched == 0) {
            throw PatchException("No AMOLED theme color resources matched")
        }
    }
}
