/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yanosik.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "pro_subscription"
private const val PRODUCT = "app.yanosik.pro.sub.management"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Removes ads and enables the floating widget, dynamic island, route editing " +
        "and tab customization. Points, quests and rankings are not included.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.YANOSIK)
    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
