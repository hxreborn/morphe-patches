/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.focusnow.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "pro"
private const val PRODUCT = "fm_pro_v0"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks FocusNow Pro.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.FOCUS_NOW)

    dependsOn(removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
