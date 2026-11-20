/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "Pro"
private const val PRODUCT = "com.risesci.nyx.subscriptions.annual"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the energy schedule, habit tools, smart alarm and progress insights. " +
        "Requires a RISE account.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.RISE)
    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
