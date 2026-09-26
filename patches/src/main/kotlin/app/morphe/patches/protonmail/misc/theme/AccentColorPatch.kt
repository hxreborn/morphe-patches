/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonmail.misc.theme.webview.webSettingsThemePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.transformBrandColors

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(webSettingsThemePatch)

    execute {
        transformBrandColors(BRAND_COLORS)
    }
}
