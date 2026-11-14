/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.slots

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.candycrushsaga.shared.candyCrushModifiersPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.replaceStub

private const val MODIFIERS = "Lapp/hxreborn/extension/candycrushsaga/GameModifiers;"

@Suppress("unused")
val unlockBoosterSlotsPatch = bytecodePatch(
    name = "Unlock booster slots",
    description = "Unlocks booster slots at any level. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(candyCrushModifiersPatch)

    execute {
        mutableClassDefBy(MODIFIERS).replaceStub("slotsEnabled", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
