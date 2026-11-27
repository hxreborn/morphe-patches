/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rubberbands.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

private const val ENTITLEMENT = "unlock"
private const val PRODUCT = "rb_unlocked"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks running and logging workouts, progress tracking and personal records. " +
        "AI workout generation is not included.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.RUBBER_BANDS)

    dependsOn(removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
