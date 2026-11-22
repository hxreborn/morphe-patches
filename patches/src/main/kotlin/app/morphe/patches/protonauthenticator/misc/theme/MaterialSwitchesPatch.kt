/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.drawSwitchAsView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.extension.protonMaterialSwitchViewExtensionPatch

private const val ENABLED_PARAMETER = 4
private const val COMPOSER_PARAMETER = 7

@Suppress("unused")
val materialSwitchesPatch: Patch = patch(
    name = "Material 3 switches",
    description = "Adds check and close icons to switches.",
) {
    dependsOn(protonMaterialSwitchViewExtensionPatch)
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchMetadata("materialSwitches", materialSwitchesPatch, withDescription = true)
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)
        drawSwitchAsView(
            MaterialSwitchFingerprint.method,
            COMPOSER_PARAMETER,
            null,
            ::androidViewFingerprint,
            ENABLED_PARAMETER,
        )
    }
}
