/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

val removePairipProtectionPatch = bytecodePatch {
    compatibleWith(
        AppCompatibilities.ECHOGRAM,
        AppCompatibilities.HINDU_CALENDAR,
        AppCompatibilities.QURANIFY,
        AppCompatibilities.RATEGLANCE,
        AppCompatibilities.VLLO,
    )

    execute {
        InitializeLicenseCheckFingerprint.matchSingle().method.returnEarly()
    }
}
