/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData

private val disableBugsnagReportingPatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("com.bugsnag.android.ENABLED_RELEASE_STAGES", "none")
            document.putApplicationMetaData("com.bugsnag.android.AUTO_TRACK_SESSIONS", "false")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, mParticle, CleverTap, NPAW, Bugsnag, Firebase Analytics and Facebook from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.SHAHID)
    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch, disableBugsnagReportingPatch)

    execute {
        AppsFlyerInitFingerprint.method.addInstruction(0, "return-object p0")
        AppsFlyerReportingFingerprint.matchAll(8..8).forEach { it.method.returnEarly() }

        MParticleConfigRequestFingerprint.method.returnEarly()
        MParticleUploadFingerprint.method.returnEarly(202)

        NpawAnalyticsEnabledFingerprint.method.returnEarly(false)
        NpawEventSendFingerprint.method.returnEarly()
        CleverTapQueueFlushFingerprint.method.returnEarly()
    }
}
