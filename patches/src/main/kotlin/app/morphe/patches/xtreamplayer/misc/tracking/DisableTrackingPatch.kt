/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Disables ML Kit usage reporting.",
) {
    compatibleWith(AppCompatibilities.XTREAM_PLAYER)

    execute {
        CommonLoggingFingerprint.matchSingle().method.returnEarly()
        TranslationLoggingFingerprint.matchSingle().method.returnEarly()
    }
}
