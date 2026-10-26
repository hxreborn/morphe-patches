/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import app.morphe.patches.watchit.misc.piracy.disablePiracyCheckPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Facebook app events and Brightcove video " +
        "analytics from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.WATCH_IT)

    dependsOn(
        disablePiracyCheckPatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableFirebaseSessionsPatch,
    )

    execute {
        VideoAnalyticsSubmitFingerprint.matchSingle().method.returnEarly()
        InteractivityAnalyticsSubmitFingerprint.matchSingle().method.returnEarly()
        FacebookAppEventsRequestFingerprint.matchSingle().method.returnEarly(null)
    }
}
