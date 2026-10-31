/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiobookunlimited.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.hermes.hermesPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.expo.disableOtaUpdatesPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatLifetimeEntitlement

private const val ENTITLEMENT = "Unlimited Books"
private const val PRODUCT = "kindle_lifetime_notrial"

private const val ONBOARDING_REPLACES_WITH_PAYWALL =
    "44 02 03 01 B9 91 01 71 06 01 00 6E 01 02 03 01"
private const val ONBOARDING_REPLACES_WITH_HOME =
    "44 02 03 01 B9 91 01 5E A7 00 00 6E 01 02 03 01"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Unlimited Books and skips the subscription offer shown after onboarding. " +
        "Disables OTA updates.",
) {
    compatibleWith(AppCompatibilities.AUDIOBOOK_UNLIMITED)
    dependsOn(
        removePairipProtectionPatch,
        disableOtaUpdatesPatch,
        hermesPatch {
            setOf(ONBOARDING_REPLACES_WITH_PAYWALL to ONBOARDING_REPLACES_WITH_HOME)
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatLifetimeEntitlement(ENTITLEMENT, PRODUCT)
    }
}
