/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.misc.moves

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.blossomblast.shared.MODIFIERS
import app.morphe.patches.blossomblast.shared.blossomBlastModifiersPatch
import app.morphe.patches.blossomblast.shared.replaceStringArrayStub
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val INCREASE_STARTING_MOVES_SKILL = 2
private const val EXTRA_MOVES = 5

@Suppress("unused")
val addExtraMovesPatch = bytecodePatch(
    name = "Add extra moves",
    description = "Starts every level with five extra moves. Requires a standard install on a 64-bit ARM device.",
) {
    compatibleWith(AppCompatibilities.BLOSSOM_BLAST_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(blossomBlastModifiersPatch)

    execute {
        mutableClassDefBy(MODIFIERS).replaceStringArrayStub(
            "movesCommands",
            listOf("skill.add $INCREASE_STARTING_MOVES_SKILL $EXTRA_MOVES"),
        )
    }
}
