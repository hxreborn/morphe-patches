/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.misc.boosters

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.blossomblast.shared.MODIFIERS
import app.morphe.patches.blossomblast.shared.blossomBlastModifiersPatch
import app.morphe.patches.blossomblast.shared.replaceStringArrayStub
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.replaceStub

private const val BOOSTER_TARGET = 999
private val BOOSTER_NAMES = listOf("shovel", "water", "color")

@Suppress("unused")
val addBoostersPatch = bytecodePatch(
    name = "Add boosters",
    description = "Tops up each booster to 999 every time the game starts.",
) {
    compatibleWith(AppCompatibilities.BLOSSOM_BLAST_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(blossomBlastModifiersPatch)

    execute {
        mutableClassDefBy(MODIFIERS).apply {
            replaceStringArrayStub("boosterNames", BOOSTER_NAMES)
            replaceStub("boosterTarget", 1, """
                const/16 v0, $BOOSTER_TARGET
                return v0
            """)
        }
    }
}
