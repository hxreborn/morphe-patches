/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.internetspeedmeter.misc.protection.removeAppProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics and Sessions from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.INTERNET_SPEED_METER_LITE)

    dependsOn(
        removeAppProtectionPatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
    )
}
