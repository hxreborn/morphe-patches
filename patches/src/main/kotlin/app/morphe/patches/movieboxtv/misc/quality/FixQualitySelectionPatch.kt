/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.patch.patch
import app.morphe.patches.movieboxtv.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm

@Suppress("unused")
val fixQualitySelectionPatch = patch(
    name = "Fix quality selection",
    description = "Makes the quality choices in the player switch the stream to 480p, 720p or 1080p. " +
        "Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(unlockPremiumPatch)
    availability(requireArm)

    execute {
        installQualityStreams()
    }
}
