/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.visualtraceroute

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MaterialIcons
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarIcon
import app.morphe.patches.netanalyzer.shared.emptyGroupSlot
import app.morphe.patches.netanalyzer.shared.hostToolsPatch
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val visualTraceroutePatch = bytecodePatch(
    name = "Visual traceroute",
    description = "Shows the hops of the current or last traceroute on an OpenStreetMap map. " +
        "Map tiles load from tile.openstreetmap.org.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, hostToolsPatch)

    execute {
        addToolbarIcon(emptyGroupSlot("ToolsTracerouteMenu"), "map", MaterialIcons.MAP, "Visual", "VisualTracerouteActivity")
        addMenuEntry(MenuSection.TOOLS, "Visual Traceroute", "VisualTracerouteActivity")
        addFeatureActivity("ProbeDetailsActivity")
    }
}
