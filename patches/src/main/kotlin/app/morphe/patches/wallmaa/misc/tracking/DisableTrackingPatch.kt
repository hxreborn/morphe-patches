/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wallmaa.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops the LevelPlay ad mediation SDK and its ad networks from starting. " +
        "Ads no longer load.",
) {
    compatibleWith(AppCompatibilities.WALLMAA)

    execute {
        LevelPlayInitFingerprint.matchSingle().method.apply {
            val nullRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    const/4 v$nullRegister, 0x0
                    invoke-interface { p2, v$nullRegister }, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                    return-void
                """,
            )
        }
    }
}
