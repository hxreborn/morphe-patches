/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.privacynotes.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/privacynotes/ProUnlock;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro features, unlimited devices and larger file uploads. " +
        "The server still blocks note history and caps storage at 50 MB.",
) {
    compatibleWith(AppCompatibilities.PRIVACY_NOTES)
    extendWith("extensions/extension.mpe")

    execute {
        PageStartedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->unlock(Landroid/webkit/WebView;)V",
        )
    }
}
