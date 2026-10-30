/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val TIME_MACHINE_ERASE_LIMIT = 1000

@Suppress("unused")
val raiseTimeMachineEraseLimitPatch = bytecodePatch(
    name = "Raise Time Machine erase limit",
    description = "Raises the limit on messages erased at once with Time Machine to 1000.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        TimeMachineEraseLimitFingerprint.matchSingle().method.returnEarly(TIME_MACHINE_ERASE_LIMIT)
    }
}
