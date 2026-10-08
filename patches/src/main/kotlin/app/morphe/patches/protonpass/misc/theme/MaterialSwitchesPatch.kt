/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.drawSwitchAsView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val COMPOSER_PARAMETER = 5

@Suppress("unused")
val materialSwitchesPatch = bytecodePatch(
    name = "Material 3 switches",
    description = "Shows switches in the Material 3 style with check and close icons.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)

        val match = MaterialSwitchFingerprint.matchSingle()
        val modifierCompanion = match.method.getInstruction(match.instructionMatches.first().index)
            .getReference<FieldReference>()!!

        drawSwitchAsView(match.method, COMPOSER_PARAMETER, modifierCompanion, ::androidViewFingerprint)
    }
}
