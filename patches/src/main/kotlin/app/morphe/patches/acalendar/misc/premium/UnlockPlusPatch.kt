/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.acalendar.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPlusPatch = bytecodePatch(
    name = "Unlock aCalendar+",
    description = "Unlocks public holidays, color schemes, business features and tasks, and removes ads.",
) {
    compatibleWith(AppCompatibilities.ACALENDAR)

    execute {
        IsPlusFingerprint.matchSingle().method.returnEarly(true)
    }
}
