/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.premium

import app.morphe.patches.catzy.catzyPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = catzyPatch(
    name = "Unlock premium",
    description = "Unlocks premium goals, journeys, breathing exercises, focus timers, sounds and themes.",
) {
    vipTimeCheckFingerprint("getVip_expired_at").matchSingle().method.returnEarly(true)
    vipTimeCheckFingerprint("getRemaining_time").matchSingle().method.returnEarly(true)
}
