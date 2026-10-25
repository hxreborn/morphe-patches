/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.prayertimes.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks random Adhan, unlimited personal reminders, theme colors and other " +
        "Pro settings.",
) {
    compatibleWith(AppCompatibilities.PRAYER_TIMES)

    dependsOn(removePairipProtectionPatch)

    execute {
        IsProUnlockedFingerprint.matchSingle().method.returnEarly(true)
    }
}
