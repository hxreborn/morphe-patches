/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatLifetimeEntitlement

private const val ENTITLEMENT = "MyScript Notes – Full Access"
private const val PRODUCT = "msnotes_standard_lifetime"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks unlimited notes and pages, text conversion, shape and scratch-out gestures, " +
        "PDF import and AI tools.",
) {
    compatibleWith(AppCompatibilities.MYSCRIPT_NOTES)
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatLifetimeEntitlement(ENTITLEMENT, PRODUCT)
    }
}
