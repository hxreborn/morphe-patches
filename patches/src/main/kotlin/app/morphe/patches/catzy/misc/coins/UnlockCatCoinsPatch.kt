/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.coins

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.catzy.catzyPatch

@Suppress("unused")
val unlockCatCoinsPatch = catzyPatch(
    name = "Unlimited cat coins",
    description = "Buys every store item without running out of cat coins.",
    selection = PatchAvailability.DISABLED,
) {
    CatCoinBalanceFingerprint.matchSingle().method.returnEarly(999_999_999L)
}
