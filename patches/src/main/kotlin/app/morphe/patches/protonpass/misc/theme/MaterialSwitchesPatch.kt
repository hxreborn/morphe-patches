/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.drawSwitchAsView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import app.morphe.patches.shared.misc.extension.protonMaterialSwitchViewExtensionPatch

private const val COLORS_PARAMETER = 4
private const val COMPOSER_PARAMETER = 5

@Suppress("unused")
val materialSwitchesPatch: Patch = patch(
    name = "Material 3 switches",
    description = "Shows switches in the Material 3 style with check and close icons.",
) {
    dependsOn(protonMaterialSwitchViewExtensionPatch)
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchMetadata("materialSwitches", materialSwitchesPatch, withDescription = true)
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)

        val match = MaterialSwitchFingerprint.match()
        val modifierCompanion = match.method.getInstruction(match.instructionMatches.first().index)
            .getReference<FieldReference>()!!

        drawSwitchAsView(
            match.method,
            COMPOSER_PARAMETER,
            modifierCompanion,
            ::androidViewFingerprint,
            colorsParameter = COLORS_PARAMETER,
        )
    }
}
