/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.lives

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.candycrushsaga.misc.boosters.candyCrushRewardsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.replaceStub

private const val BOOSTERS = "Lapp/hxreborn/extension/candycrushsaga/Boosters;"

@Suppress("unused")
val unlimitedLivesPatch = bytecodePatch(
    name = "Unlimited lives",
    description = "Adds 24 hours of unlimited lives every time the game starts. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(candyCrushRewardsPatch)

    execute {
        mutableClassDefBy(BOOSTERS).replaceStub("livesEnabled", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
