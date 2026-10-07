/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import app.morphe.util.matchSingle

private val disableSdkCollectionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("com.onesignal.PrivacyConsent", "ENABLE")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Facebook and OneSignal from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)
    dependsOn(
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        disableSdkCollectionPatch,
    )

    execute {
        FacebookOpenConnectionFingerprint.matchSingle().method.addInstructions(
            0,
            """
                new-instance v0, Ljava/io/IOException;
                invoke-direct { v0 }, Ljava/io/IOException;-><init>()V
                throw v0
            """,
        )
    }
}
