/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.layout.navigation

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.yuka.misc.pairip.guardVirtualizedTasksPatch
import app.morphe.util.findElementByAttributeValueOrThrow

private const val BOTTOM_NAVIGATION_MENU = "res/menu/menu_bottom_navigation_main_activity.xml"
private const val SEARCH_ITEM_ID = "@id/bottom_navigation_search"

@Suppress("unused")
val hideSearchTabPatch = patch(
    name = "Hide Search tab",
    description = "Hides the Search tab from the bottom bar.",
) {
    compatibleWith(AppCompatibilities.YUKA)

    availability(requireArm64)

    dependsOn(guardVirtualizedTasksPatch, removePairipProtectionPatch)

    execute {
        document(BOTTOM_NAVIGATION_MENU).use { document ->
            val searchItem = document.getElementsByTagName("item")
                .findElementByAttributeValueOrThrow("android:id", SEARCH_ITEM_ID)
            searchItem.parentNode.removeChild(searchItem)
        }
    }
}
