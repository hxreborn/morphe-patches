/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.onboarding

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val skipSignUpPatch = bytecodePatch(
    name = "Skip sign-up screen",
    description = "Opens the app without signing in. Sign in later from the menu.",
) {
    compatibleWith(AppCompatibilities.TRADINGVIEW)

    execute {
        OnboardingAvailabilityFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )
    }
}
