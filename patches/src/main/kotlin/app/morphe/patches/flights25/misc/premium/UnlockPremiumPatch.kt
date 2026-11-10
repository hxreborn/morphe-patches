/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

private const val PRODUCT = "unlimited_access_yearly_flight_tracker"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium features. AI imports, Gmail scans and automatic calendar imports are not unlocked.",
) {
    compatibleWith(AppCompatibilities.FLIGHTS25)
    extendWith("extensions/extension.mpe")

    dependsOn(removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements(
            "unlimited_access" to PRODUCT,
            "flights25_business_access" to PRODUCT,
            "import_flights" to PRODUCT,
        )
    }
}
