/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musixmatch.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "community_premium"
private const val PRODUCT = "com.musixmatch.offers.removeads.autorenewable.oneyear.1582713041"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks offline lyrics, animated backgrounds and Android Auto lyrics.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.MUSIXMATCH)
    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
