/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.presets

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.ultimea.misc.equalizer.DeviceActivityFingerprint
import app.morphe.patches.ultimea.misc.equalizer.fineEqualizerPatch
import app.morphe.patches.ultimea.misc.settings.markPatchApplied

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/ultimea/equalizer/LocalPresets;"

@Suppress("unused")
val localEqualizerPresetsPatch: BytecodePatch = bytecodePatch(
    name = "Local equalizer presets",
    description = "Adds named local curves with duplicate, export and import actions.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(fineEqualizerPatch)

    execute {
        markPatchApplied("localEqualizerPresets", localEqualizerPresetsPatch)
        val activity = DeviceActivityFingerprint.matchSingle().method
        val parent = mutableClassDefBy(activity.definingClass).superclass!!
        ActivityResultFingerprint(parent).matchSingle().method.addInstructions(0,
            "invoke-static/range { p0 .. p3 }, $EXTENSION_CLASS_DESCRIPTOR->onActivityResult(Landroid/app/Activity;IILandroid/content/Intent;)V")
    }
}
