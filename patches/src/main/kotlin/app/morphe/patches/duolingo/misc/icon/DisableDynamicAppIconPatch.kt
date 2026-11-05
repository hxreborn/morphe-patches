/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.icon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.duolingo.misc.settings.PATCH_SETTINGS_CLASS
import app.morphe.patches.duolingo.misc.settings.markPatchApplied
import app.morphe.patches.duolingo.misc.settings.patchesSettingsPatch
import app.morphe.patches.duolingo.shared.integrity.disableLoginIntegrityPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableDynamicAppIconPatch: BytecodePatch = bytecodePatch(
    name = "Disable dynamic app icon",
    description = "Keeps the default launcher icon.",
    default = false,
) {
    compatibleWith(AppCompatibilities.DUOLINGO)

    dependsOn(disableLoginIntegrityPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("disableDynamicAppIcon", disableDynamicAppIconPatch)

        val (_, _, defaultDisplayWrite) = DefaultAppDisplayFingerprint.instructionMatches
        val defaultDisplay = defaultDisplayWrite.instruction.getReference<FieldReference>()!!
        val switchMethod = AppIconSwitchFingerprint.method
        val displayType = switchMethod.parameterTypes.first().toString()
        if (defaultDisplay.type != displayType) {
            throw PatchException("Default app display ${defaultDisplay.type} does not match switch parameter $displayType")
        }

        switchMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $PATCH_SETTINGS_CLASS->isDynamicAppIconDisabled()Z
                move-result v0
                if-eqz v0, :switch_icon
                sget-object p1, $defaultDisplay
            """,
            ExternalLabel("switch_icon", switchMethod.getInstruction(0)),
        )
    }
}
