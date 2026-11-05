/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.download

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}download/DownloadWatermark;"

@Suppress("unused")
val removeWatermarkPatch: BytecodePatch = bytecodePatch(
    name = "Remove watermark",
    description = "Saves downloaded BeReals at full size without the BeReal logo.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        markFeaturePatched(EXTENSION_CLASS, removeWatermarkPatch)

        val method = DownloadImageFingerprint.matchSingle().method
        method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS->removeLogo()Z
                move-result v0
                if-eqz v0, :keep
                return-object p1
            """,
            ExternalLabel("keep", method.getInstruction(0)),
        )
    }
}
