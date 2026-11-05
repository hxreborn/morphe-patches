/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mangatime.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

private const val ENTITLEMENT = "MangaTime Plus"
private const val PRODUCT = "mt_plus_1"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Removes ads and chapter limits, and unlocks reading history, binge mode and section settings. " +
        "AI recommendation refresh and custom explore views are not unlocked.",
) {
    compatibleWith(AppCompatibilities.MANGATIME)
    dependsOn(removePairipProtectionPatch)
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
