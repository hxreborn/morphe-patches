/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops usage analytics and crash reporting.",
) {
    compatibleWith(AppCompatibilities.GROUND_NEWS)
    dependsOn(
        disableAnalyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
    )
    execute {
        AnalyticsOptOutFingerprints.forEach { it.matchSingle().method.returnEarly(true) }
        TelemetryFingerprints.forEach { it.matchSingle().method.returnEarly() }
        AmplitudeEventFingerprint.matchAll(2..2).forEach { it.method.returnEarly() }
    }
}
