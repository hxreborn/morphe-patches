/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hinducalendar.tracking

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

@Suppress("unused")
val disableTrackingPatch = resourcePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.HINDU_CALENDAR)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        disableAnalyticsCollectionPatch,
    )
}
