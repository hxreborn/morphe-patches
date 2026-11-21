/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.sheytoon.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "select"
private const val PRODUCT = "sheytoon_select_monthly"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Sheytoon Select: unlimited likes, superlikes and quick notes, incognito mode, " +
        "Second Look, and unblurred photos of the people who liked or viewed you.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.SHEYTOON)
    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
