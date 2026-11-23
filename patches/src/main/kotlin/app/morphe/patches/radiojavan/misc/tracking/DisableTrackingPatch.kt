/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.radiojavan.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics and Firebase Sessions from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.RADIO_JAVAN)

    dependsOn(disableAnalyticsCollectionPatch, disableCrashlyticsCollectionPatch, disableFirebaseSessionsPatch)
}
