/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blurwall.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "premium"
private const val PRODUCT = "blurwall_premium"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the HalfBlur effect.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.BLURWALL)

    dependsOn(removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
