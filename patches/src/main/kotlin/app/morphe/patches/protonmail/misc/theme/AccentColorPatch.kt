/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.protonmail.misc.theme.webview.webSettingsThemePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.protonAccentColorPickerExtensionPatch
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.proton.transformBrandColors

@Suppress("unused")
val accentColorPatch: Patch = patch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(protonAccentColorPickerExtensionPatch)
    dependsOn(webSettingsThemePatch)

    execute {
        markPatchMetadata("accentColor", accentColorPatch)
        transformBrandColors(BRAND_COLORS)
    }
}
