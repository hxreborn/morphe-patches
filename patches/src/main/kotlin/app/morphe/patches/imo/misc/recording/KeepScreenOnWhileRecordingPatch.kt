/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.recording

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val keepScreenOnWhileRecordingPatch = bytecodePatch(
    name = "Keep screen on while recording",
    description = "Keeps the screen on while recording a voice message.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        RecordDisableScreenOffFingerprint.matchSingle().method.returnEarly(true)
    }
}
