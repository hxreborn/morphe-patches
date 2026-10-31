/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.pixlr.ads.adRemovalEntitlementPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

private val HOME_LAYOUTS = listOf(
    "res/layout/activity_startup.xml",
    "res/layout/activity_old_startup.xml",
    "res/layout-sw600dp/activity_startup.xml",
    "res/layout-sw600dp-port/activity_startup.xml",
)

private val hidePremiumButtonPatch = resourcePatch {
    execute {
        HOME_LAYOUTS.forEach { layout ->
            document(layout).use { document ->
                document.getElementsByTagName("*")
                    .findElementByAttributeValueOrThrow("android:id", "@id/premium_button")
                    .setAttribute("android:visibility", "gone")
            }
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium tools and content, and removes ads and subscription screens. " +
        "AI tools need a signed-in account with credits.",
) {
    compatibleWith(AppCompatibilities.PIXLR)

    dependsOn(adRemovalEntitlementPatch, hidePremiumButtonPatch)

    execute {
        HasSubscriptionFingerprint.matchSingle().method.returnEarly(true)
    }
}
