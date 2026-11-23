/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.systembars

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.uptcl.misc.fix.signature.trustInstalledCertificatePatch
import app.morphe.patches.shared.misc.extension.uptclMatchSystemBarColorExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/uptcl/WindowBackground;"

@Suppress("unused")
val matchSystemBarColorPatch = patch(
    name = "Match system bar color",
    description = "Colors the status bar and navigation bar white to match the app's screens.",
) {
    dependsOn(uptclMatchSystemBarColorExtensionPatch)
    compatibleWith(AppCompatibilities.UPTCL)
    dependsOn(trustInstalledCertificatePatch)

    execute {
        BaseActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->paintWhite(Landroid/app/Activity;)V",
        )
    }
}
