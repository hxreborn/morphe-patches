/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.movieboxtv.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.inputStreamFromBundledResource

@Suppress("unused")
val fixQualitySelectionPatch = patch(
    name = "Fix quality selection",
    description = "Makes the quality choices in the player switch the stream to 480p, 720p or 1080p.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(unlockPremiumPatch)
    extendWith {
        inputStreamFromBundledResource("moviebox", "dashhook.dex")
            ?: throw PatchException("Missing bundled resource: moviebox/dashhook.dex")
    }

    execute {
        installQualityStreams()
    }
}
