/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.drawSwitchAsView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.matchSingle

private const val ENABLED_PARAMETER = 4
private const val COMPOSER_PARAMETER = 7

@Suppress("unused")
val materialSwitchesPatch = bytecodePatch(
    name = "Material 3 switches",
    description = "Adds check and close icons to switches.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)
        drawSwitchAsView(
            MaterialSwitchFingerprint.matchSingle().method,
            COMPOSER_PARAMETER,
            null,
            ::androidViewFingerprint,
            ENABLED_PARAMETER,
        )
    }
}
