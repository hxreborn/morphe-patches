/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.showly.layout.glass

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedGlassNavigationExtensionPatch
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.toJavaClassName
import app.morphe.util.findElementByAttributeValueOrThrow

private const val GLASS_VIEW_CLASS = "Lapp/hxreborn/extension/shared/GlassNavigationView;"
private val GLASS_VIEW_TAG = GLASS_VIEW_CLASS.toJavaClassName()

@Suppress("unused")
val glassNavigationBarPatch = patch(
    name = "Glass navigation bar",
    description = "Adds a translucent blurred navigation bar. Uses translucency on Android 11 and earlier.",
) {
    compatibleWith(AppCompatibilities.SHOWLY)
    dependsOn(sharedGlassNavigationExtensionPatch)

    execute {
        mutableClassDefBy(GLASS_VIEW_CLASS)
            .replaceResourceIdStubs(ResourceType.ID, mapOf("sourceId" to "navigationHost"))
        document("res/drawable/selector_bottom_menu.xml").use { document ->
            document.getElementsByTagName("item")
                .findElementByAttributeValueOrThrow("android:state_checked", "false")
                .setAttribute("android:color", "?colorBottomMenuItemChecked")
        }
        document("res/layout/activity_main.xml").use { document ->
            val menu = document.getElementsByTagName("*")
                .findElementByAttributeValueOrThrow("android:id", "@id/bottomMenuView")
            val parent = menu.parentNode
            val wrapper = document.createElement(GLASS_VIEW_TAG)
            wrapper.setAttribute("android:layout_width", menu.getAttribute("android:layout_width"))
            wrapper.setAttribute("android:layout_height", menu.getAttribute("android:layout_height"))
            parent.replaceChild(wrapper, menu)
            wrapper.appendChild(menu)
            menu.setAttribute("android:layout_height", "match_parent")
        }
    }
}
