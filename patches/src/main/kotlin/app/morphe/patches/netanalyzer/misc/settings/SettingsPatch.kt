/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.settings

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds a Settings screen with the theme, Wi-Fi scan frequency and 6 GHz scan options.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addMenuEntry(MenuSection.SETTINGS, "Settings", "SettingsActivity")
    }
}
