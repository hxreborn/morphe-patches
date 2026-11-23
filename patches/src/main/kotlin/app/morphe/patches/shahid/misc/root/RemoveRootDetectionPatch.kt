/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.root

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeRootDetectionPatch = patch(
    name = "Remove root detection",
    description = "Opens the app and plays videos on rooted devices.",
) {
    compatibleWith(AppCompatibilities.SHAHID)

    execute {
        RootDetectedFingerprint.method.returnEarly(false)
    }
}
