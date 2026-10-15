/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.pingstatistics

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MaterialIcons
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarIcon
import app.morphe.patches.netanalyzer.shared.emptyGroupSlot
import app.morphe.patches.netanalyzer.shared.hostToolsPatch
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val pingStatisticsPatch = bytecodePatch(
    name = "Ping statistics",
    description = "Adds a bar chart of the current or last ping's round-trip times with min, median, max " +
        "and lost packets.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, hostToolsPatch)

    execute {
        addToolbarIcon(emptyGroupSlot("ToolsPingMenu"), "bar_chart", MaterialIcons.BAR_CHART, "Statistics", "PingStatisticsActivity")
        addMenuEntry(MenuSection.TOOLS, "Ping Statistics", "PingStatisticsActivity")
    }
}
