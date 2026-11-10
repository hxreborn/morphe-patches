/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val bypassSessionExpiryPatch = bytecodePatch(
    name = "Bypass session expiry",
    description = "Stops the session expired prompt and toast.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    execute {
        ReLoginPromptFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                return-object v0
            """,
        )

        ReLoginToastFingerprint.matchSingle().apply {
            val (_, _, toastCall) = instructionMatches
            method.removeInstruction(toastCall.index)
        }
    }
}
