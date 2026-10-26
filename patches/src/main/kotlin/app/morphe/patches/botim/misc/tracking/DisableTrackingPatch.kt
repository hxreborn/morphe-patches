/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.botim.misc.signin.fixSignInPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Adjust and MoEngage from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.BOTIM)

    dependsOn(
        fixSignInPatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableFirebaseSessionsPatch,
    )

    execute {
        AdjustInitFingerprint.matchSingle().method.returnEarly()
        MoEngageInitFingerprint.matchSingle().method.returnEarly()
        CrashlyticsEnableCollectionFingerprint.matchSingle().method.returnEarly()
    }
}
