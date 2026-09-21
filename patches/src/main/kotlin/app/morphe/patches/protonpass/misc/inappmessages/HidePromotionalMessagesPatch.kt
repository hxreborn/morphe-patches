/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.inappmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle

@Suppress("unused")
val hidePromotionalMessagesPatch = bytecodePatch(
    name = "Hide promotional messages",
    description = "Hides promotional banners, offers and pop-up messages.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)

    execute {
        StoreInAppMessagesFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister()

            addInstructions(
                0,
                """
                    sget-object v$register, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                    return-object v$register
                """,
            )
        }
    }
}
