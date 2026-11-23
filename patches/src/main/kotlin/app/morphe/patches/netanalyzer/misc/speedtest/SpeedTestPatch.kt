/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.speedtest

import app.morphe.patcher.patch.patch
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.extension.netanalyzerChannelUsageActivityExtensionPatch
import app.morphe.patches.shared.misc.extension.netanalyzerChartViewExtensionPatch
import app.morphe.patches.shared.misc.extension.netanalyzerSpeedTestExtensionPatch

@Suppress("unused")
val speedTestPatch = patch(
    name = "Speed test",
    description = "Adds an internet speed test with download and upload charts and a test history.",
) {
    dependsOn(netanalyzerChannelUsageActivityExtensionPatch, netanalyzerChartViewExtensionPatch, netanalyzerSpeedTestExtensionPatch)
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addMenuEntry(MenuSection.TOOLS, "Speed Test", "SpeedTestActivity")
        addFeatureActivity("SpeedHistoryActivity")
    }
}
