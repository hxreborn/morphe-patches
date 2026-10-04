/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.fddb.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the weekly report, intermittent fasting, the calorie and nutrient planners, and custom nutrient targets.",
) {
    compatibleWith(AppCompatibilities.FDDB)

    execute {
        val hasPremiumMatch = HasPremiumFingerprint.matchSingle()
        hasPremiumMatch.method.returnEarly(true)
        premiumMembershipCheckFingerprint(hasPremiumMatch.originalMethod).matchSingle().method.returnEarly(true)
    }
}
