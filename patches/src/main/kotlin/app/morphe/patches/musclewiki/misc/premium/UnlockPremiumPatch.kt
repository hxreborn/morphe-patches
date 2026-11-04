/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

private const val ENTITLEMENT = "basic"
private const val PRODUCT = "basic"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks MuscleWiki Premium and removes ads. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.MUSCLEWIKI)
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
