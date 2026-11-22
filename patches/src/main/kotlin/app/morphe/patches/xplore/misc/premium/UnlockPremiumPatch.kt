/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xplore.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.xplore.misc.pairip.restoreVirtualizedMethodsPatch

val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium features and removes ads.",
) {
    compatibleWith(AppCompatibilities.XPLORE)
    dependsOn(restoreVirtualizedMethodsPatch)

    execute {
        HasDonationFingerprint.method.returnEarly(true)
        FeatureRestrictionFingerprint.method.returnEarly(false)
    }
}
