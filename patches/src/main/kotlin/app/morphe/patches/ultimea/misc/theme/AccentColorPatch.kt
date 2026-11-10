/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.ultimea.misc.settings.markPatchApplied
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/ultimea/AccentColor;"

@Suppress("unused")
val accentColorPatch: BytecodePatch = bytecodePatch(
    name = "Accent color",
    description = "Changes accent colors with a chosen color or Material You.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, resourcePatch {
        execute {
            val library = dartAotLibrary()
            val image = ThemeElfImage(library.readBytes())
            val matches = AccentInstructions(image.bytes, image.codeStart, image.codeEnd).match()
            val palette = AccentHooks.apply(image, matches)
            library.writeBytes(image.bytes)
            get("assets/hx_ultimea/accent_colors").apply {
                parentFile.mkdirs()
                writeBytes(palette)
            }
        }
    })

    execute {
        markPatchApplied("accentColor", accentColorPatch)
        ActivityCreateFingerprint.matchSingle().method.addInstructions(0,
            "invoke-static { p0 }, $EXTENSION_CLASS->attach(Landroid/app/Activity;)V")
    }
}
