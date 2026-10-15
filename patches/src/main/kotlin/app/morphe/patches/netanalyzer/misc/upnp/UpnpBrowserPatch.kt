/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.upnp

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val upnpBrowserPatch = bytecodePatch(
    name = "UPnP browser",
    description = "Lists the UPnP devices on the Wi-Fi network with their model, manufacturer, embedded " +
        "devices and services.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addMenuEntry(MenuSection.NETWORK, "UPnP Browser", "UpnpActivity")
        addFeatureActivity("UpnpServicesActivity")
    }
}
