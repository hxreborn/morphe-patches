/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.lanhistory

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.MainActivityOnCreateFingerprint
import app.morphe.patches.netanalyzer.shared.MaterialIcons
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarIcon
import app.morphe.patches.netanalyzer.shared.emptyGroupSlot
import app.morphe.patches.netanalyzer.shared.lanScanObserverPatch
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle

@Suppress("unused")
val lanScanHistoryPatch = bytecodePatch(
    name = "LAN scan history",
    description = "Saves each finished LAN scan and its devices, lets you star, delete or clear saved scans, " +
        "and scans a custom IP range.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, lanScanObserverPatch)

    execute {
        addToolbarIcon(emptyGroupSlot("LanScanMenu"), "history", MaterialIcons.HISTORY, "History", "LanHistoryActivity")
        addMenuEntry(MenuSection.NETWORK, "LAN Scan History", "LanHistoryActivity")
        addFeatureActivity("LanScanResultsActivity")
        addFeatureActivity("LanDeviceActivity")
        addFeatureActivity("LanCustomNameActivity")
        addFeatureActivity("WakeOnLanActivity")
        MainActivityOnCreateFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { }, $FEATURES_CLASS->enableLanScanHistory()V",
        )
    }
}
