/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.fddb.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks the weekly report, intermittent fasting, the calorie and nutrient planners, and custom nutrient targets.",
) {
    compatibleWith(AppCompatibilities.FDDB)

    execute {
        val hasPremiumMatch = HasPremiumFingerprint.match()
        hasPremiumMatch.method.returnEarly(true)
        premiumMembershipCheckFingerprint(hasPremiumMatch.originalMethod).method.returnEarly(true)
    }
}
