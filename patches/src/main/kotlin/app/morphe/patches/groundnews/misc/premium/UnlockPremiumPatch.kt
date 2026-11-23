/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val UNLIMITED_FEATURE_LIMIT = -1
private const val VANTAGE_SUBSCRIPTION_STATUS = "vantage"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium features.",
) {
    compatibleWith(AppCompatibilities.GROUND_NEWS)

    execute {
        UserSubscriptionStatusFingerprint.method.returnEarly(VANTAGE_SUBSCRIPTION_STATUS)
        ServerSubscriptionStatusFingerprint.method.returnEarly(VANTAGE_SUBSCRIPTION_STATUS)
        PaidFeatureEnabledFingerprint.method.returnEarly(true)
        PaidFeatureLimitFingerprint.method.returnEarly(UNLIMITED_FEATURE_LIMIT)
    }
}
