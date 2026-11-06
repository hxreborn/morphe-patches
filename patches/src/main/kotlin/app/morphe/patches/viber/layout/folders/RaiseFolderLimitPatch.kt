/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.folders

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val raiseFolderLimitPatch: BytecodePatch = bytecodePatch(
    name = "Raise folder limit",
    description = "Allows up to 10 chat folders.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("raiseFolderLimit", raiseFolderLimitPatch)
        MaxFoldersFingerprint.matchSingle().apply {
            val (maxFolders) = instructionMatches
            val register = method.getInstruction<OneRegisterInstruction>(maxFolders.index).registerA
            method.addInstructions(
                maxFolders.index + 1,
                """
                    invoke-static { v$register }, $VIBER_SETTINGS_CLASS->folderLimit(I)I
                    move-result v$register
                """,
            )
        }
    }
}
