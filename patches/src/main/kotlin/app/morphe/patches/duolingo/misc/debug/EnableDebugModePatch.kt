/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/misc/debug/EnableDebugModePatch.kt
 */
package app.morphe.patches.duolingo.misc.debug

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.duolingo.misc.settings.PATCH_SETTINGS_CLASS
import app.morphe.patches.duolingo.misc.settings.markPatchApplied
import app.morphe.patches.duolingo.misc.settings.patchesSettingsPatch
import app.morphe.patches.duolingo.shared.constructorWritingFingerprint
import app.morphe.patches.duolingo.shared.integrity.disableLoginIntegrityPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val enableDebugModePatch: BytecodePatch = bytecodePatch(
    name = "Enable debug mode",
    description = "Shows the debug menu and debug button. Turn it on in the patches menu.",
    default = false,
) {
    compatibleWith(AppCompatibilities.DUOLINGO)

    dependsOn(disableLoginIntegrityPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("enableDebugMode", enableDebugModePatch)

        val (isDebugRead, _, _) = BuildTargetFingerprint.instructionMatches
        val isDebugField = isDebugRead.instruction.getReference<FieldReference>()!!

        constructorWritingFingerprint(isDebugField).method.apply {
            addInstructions(
                instructions.lastIndex,
                """
                    invoke-static { }, $PATCH_SETTINGS_CLASS->isDebugModeEnabled()Z
                    move-result v0
                    iput-boolean v0, p0, $isDebugField
                """,
            )
        }
    }
}
