/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "pro"
private const val PRODUCT = "pro_version"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the Pro tools and removes ads.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
