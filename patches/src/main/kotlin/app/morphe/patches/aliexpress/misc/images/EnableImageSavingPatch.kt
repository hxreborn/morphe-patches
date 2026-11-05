/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/media/EnableImageSavingPatch.kt
 */
package app.morphe.patches.aliexpress.misc.images

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/aliexpress/PatchSettings;"

private const val DESCRIPTION = "Shows the save button in the full-screen image viewer."

@Suppress("unused")
val enableImageSavingPatch = bytecodePatch(
    name = "Enable image saving",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)
    dependsOn(spoofSignaturePatch)

    execute {
        markPatchApplied("enableImageSaving", DESCRIPTION)
        ImageSaveButtonFingerprint.matchAll(3..3).forEach { match ->
            val (_, _, saveButtonState) = match.instructionMatches
            val register = match.method.getInstruction<OneRegisterInstruction>(saveButtonState.index).registerA
            match.method.addInstructions(
                saveButtonState.index + 1,
                """
                    invoke-static/range { v$register .. v$register }, $PATCH_SETTINGS_CLASS->saveButtonHidden(Z)Z
                    move-result v$register
                """.trimIndent(),
            )
        }
    }
}
