/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.misc.lives

import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patches.blossomblast.shared.MODIFIERS
import app.morphe.patches.blossomblast.shared.blossomBlastModifiersPatch
import app.morphe.patches.blossomblast.shared.replaceStringArrayStub
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val LIVES_HOURS = 24
private const val SECONDS_PER_HOUR = 3600

@Suppress("unused")
val unlimitedLivesPatch = patch(
    name = "Unlimited lives",
    description = "Gives unlimited lives for 24 hours every time the game starts.",
) {
    compatibleWith(AppCompatibilities.BLOSSOM_BLAST_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(blossomBlastModifiersPatch)

    execute {
        mutableClassDefBy(MODIFIERS).replaceStringArrayStub(
            "livesCommands",
            listOf("unlimitedlives ${LIVES_HOURS * SECONDS_PER_HOUR}"),
        )
    }
}
