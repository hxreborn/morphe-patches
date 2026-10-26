/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.systembars

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.uptcl.misc.fix.signature.trustInstalledCertificatePatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/uptcl/WindowBackground;"

@Suppress("unused")
val matchSystemBarColorPatch = bytecodePatch(
    name = "Match system bar color",
    description = "Colors the status bar and navigation bar white to match the app's screens.",
) {
    compatibleWith(AppCompatibilities.UPTCL)
    dependsOn(trustInstalledCertificatePatch)
    extendWith("extensions/extension.mpe")

    execute {
        BaseActivityOnCreateFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->paintWhite(Landroid/app/Activity;)V",
        )
    }
}
