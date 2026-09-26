/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.CORE_BRAND_COLORS
import app.morphe.patches.shared.misc.proton.transformBrandColors

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        transformBrandColors(CORE_BRAND_COLORS)
    }
}
