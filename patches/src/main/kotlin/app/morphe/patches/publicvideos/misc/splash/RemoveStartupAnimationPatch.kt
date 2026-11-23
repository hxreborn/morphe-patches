/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.splash

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeStartupAnimationPatch = patch(
    name = "Remove startup animation",
    description = "Opens the app without playing the animated logo on launch.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        val beginFinishSplashGate = BeginFinishSplashGateFingerprint.method

        HandleSplashEventFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-virtual { p0, v0, v0 }, ${beginFinishSplashGate.definingClass}->${beginFinishSplashGate.name}(ZZ)V
            """,
        )
    }
}
