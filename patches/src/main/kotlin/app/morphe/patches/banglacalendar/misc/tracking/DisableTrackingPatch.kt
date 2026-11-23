/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.banglacalendar.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics and Crashlytics from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.BANGLA_CALENDAR)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
    )
}
