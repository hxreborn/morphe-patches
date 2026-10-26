/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.SetCrashlyticsCollectionEnabledFingerprint
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Google Analytics, Crashlytics, Facebook App Events, Kochava and Klaviyo " +
        "from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.NARA_BABY)

    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch, disableFirebaseSessionsPatch)

    execute {
        SetCrashlyticsCollectionEnabledFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x0")
        KochavaStartFingerprint.matchSingle().method.returnEarly()
        KlaviyoInitializeFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, Lcom/klaviyo/analytics/Klaviyo;->INSTANCE:Lcom/klaviyo/analytics/Klaviyo;
                return-object v0
            """,
        )
    }
}
