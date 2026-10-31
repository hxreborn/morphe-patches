/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.pixlr.ads.adRemovalEntitlementPatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium tools and content, and removes ads and subscription screens. " +
        "AI tools need a signed-in account with credits.",
) {
    compatibleWith(AppCompatibilities.PIXLR)

    dependsOn(adRemovalEntitlementPatch)

    execute {
        HasSubscriptionFingerprint.matchSingle().method.returnEarly(true)
    }
}
