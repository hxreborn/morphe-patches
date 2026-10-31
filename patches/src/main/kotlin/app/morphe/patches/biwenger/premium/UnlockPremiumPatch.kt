/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.biwenger.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.biwenger.shared.PageStartedFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities

private const val INSTALL = "Lapp/hxreborn/extension/biwenger/PremiumLeagues;->install(Landroid/webkit/WebView;)V"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the Premium league settings that the server accepts. " +
        "The server refuses custom scoring, release clauses, substitutes and player tracking. " +
        "Actions that cost credits still charge you, with no confirmation.",
) {
    compatibleWith(AppCompatibilities.BIWENGER)

    extendWith("extensions/extension.mpe")

    execute {
        PageStartedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $INSTALL",
        )
    }
}
