/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.videoeditor.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro features.",
) {
    compatibleWith(AppCompatibilities.VIDEO_EDITOR)

    execute {
        IsProUserFingerprint.method.returnEarly(true)
    }
}
