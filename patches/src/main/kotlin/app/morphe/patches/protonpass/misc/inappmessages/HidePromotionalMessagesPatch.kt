/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.inappmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.PASS_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.markPatchApplied

@Suppress("unused")
val hidePromotionalMessagesPatch: Patch = patch(
    name = "Hide promotional messages",
    description = "Hides promotional banners, offers and pop-up messages.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied(PASS_SETTINGS_CLASS, "hidePromotionalMessages", hidePromotionalMessagesPatch)

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
