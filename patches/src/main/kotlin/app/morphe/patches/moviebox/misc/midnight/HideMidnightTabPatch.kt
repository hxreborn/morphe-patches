/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moviebox.misc.midnight

import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.moviebox.misc.allinone.allInOnePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

private const val HIDDEN_HOME_TABS_ASSET = "assets/hx_hidden_home_tabs"
private const val MIDNIGHT_TAB_CODE = "Midnight"

@Suppress("unused")
val hideMidnightTabPatch = patch(
    name = "Hide Midnight tab",
    description = "Removes the Midnight 18+ tab from the home screen. Requires All-In-One.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(allInOnePatch)
    availability(requireArm64)

    execute {
        resourceFile(HIDDEN_HOME_TABS_ASSET, copy = false).writeText(MIDNIGHT_TAB_CODE)
    }
}
