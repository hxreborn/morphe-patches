/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.preload

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableVideoPreloadingPatch = patch(
    name = "Disable video preloading",
    description = "Stops preloading upcoming videos in the feed and in dramas.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        PreloadManagerAddFingerprint.method.returnEarly()
    }
}
