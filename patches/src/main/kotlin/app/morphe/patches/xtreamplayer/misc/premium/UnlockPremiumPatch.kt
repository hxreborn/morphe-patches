/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium media features and visualizers.",
) {
    compatibleWith(AppCompatibilities.XTREAM_PLAYER)

    execute {
        VerifyLicenseFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object p0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-interface {p1, p0}, Ljava/util/function/Consumer;->accept(Ljava/lang/Object;)V
                return-void
            """,
        )
        GuardActivityFingerprint.matchSingle().method.returnEarly(true)
    }
}
