/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium, removes ads and skips the upgrade screens.",
) {
    compatibleWith(AppCompatibilities.RINGTONE_MAKER)

    execute {
        PremiumUserFingerprint.matchSingle().method.returnEarly(true)
        StartPurchaseScreenFingerprint.matchSingle().method.returnEarly()
    }
}
