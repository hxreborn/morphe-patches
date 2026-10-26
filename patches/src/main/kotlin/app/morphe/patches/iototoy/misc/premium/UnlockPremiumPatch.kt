/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iototoy.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium blocks, diagrams over 25 blocks and runs longer than 15 minutes. Removes ads.",
) {
    compatibleWith(AppCompatibilities.IOTOTOY)
    dependsOn(removePairipProtectionPatch)

    execute {
        IsProductPurchasedFingerprint.matchSingle().method.returnEarly(true)
    }
}
