/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.commonports

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarAction
import app.morphe.patches.netanalyzer.shared.emptyGroupSlot
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val commonPortsPatch = bytecodePatch(
    name = "Common ports list",
    description = "Adds an editor for the ports the Common port scan checks.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, patchesMenuPatch)

    execute {
        addToolbarAction(emptyGroupSlot("PortscanSettingsRow"), "Edit", "CommonPortsActivity")
        addMenuEntry(MenuSection.SETTINGS, "Common Ports", "CommonPortsActivity")
    }
}
