/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.bonjour

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val bonjourBrowserPatch = bytecodePatch(
    name = "Bonjour browser",
    description = "Lists the Bonjour services announced on the Wi-Fi network, grouped by type, with each " +
        "service's host, address, port and TXT records.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addMenuEntry(MenuSection.NETWORK, "Bonjour Browser", "BonjourActivity")
        addFeatureActivity("BonjourServiceActivity")
    }
}
