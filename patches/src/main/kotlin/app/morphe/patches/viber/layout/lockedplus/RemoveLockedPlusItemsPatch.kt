/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.lockedplus

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.asSequence
import org.w3c.dom.Element

@Suppress("unused")
val removeLockedPlusItemsPatch = resourcePatch(
    name = "Remove locked Plus items",
    description = "Removes Invisible status options, Plus badge settings and locked Plus entries.",
    default = false,
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch)

    execute {
        mapOf(
            "fragment_online_read_settings" to listOf(
                "online_status_invisible_btn",
                "online_status_invisible_btn_divider",
                "read_status_invisible_btn",
                "read_status_invisible_btn_divider",
                "read_status_description",
            ),
            "fragment_viber_plus_settings" to listOf(
                "hide_badge_btn",
                "hide_badge_divider",
                "invisible_mode_btn",
                "invisible_mode_divider",
                "support_btn",
                "support_divider",
            ),
        ).forEach { (layout, ids) ->
            document("res/layout/$layout.xml").use { document ->
                ids.forEach { id ->
                    val rows = document.getElementsByTagName("*").asSequence()
                        .filterIsInstance<Element>()
                        .filter { it.getAttribute("android:id") == "@id/$id" }.toList()
                    if (rows.size != 1) {
                        throw PatchException("Expected one @id/$id in res/layout/$layout.xml, found ${rows.size}")
                    }
                    val row = rows.single()
                    val container = document.createElement("FrameLayout").apply {
                        setAttribute("android:layout_width", "match_parent")
                        setAttribute("android:layout_height", "wrap_content")
                        setAttribute("android:visibility", "gone")
                    }
                    row.parentNode.replaceChild(container, row)
                    container.appendChild(row)
                }
                if (layout == "fragment_online_read_settings") {
                    document.documentElement.apply {
                        setAttribute("android:background", "?onlineReadButtonBackgroundColor")
                        childNodes.asSequence().filterIsInstance<Element>().single()
                            .setAttribute("android:background", "?onlineReadBackground")
                    }
                }
            }
        }
    }
}
