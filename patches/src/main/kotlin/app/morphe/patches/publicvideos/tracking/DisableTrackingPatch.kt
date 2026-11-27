/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Flurry, comScore, Firebase Analytics and Facebook from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    dependsOn(disableAnalyticsCollectionPatch)

    execute {
        AppsFlyerInitFingerprint.method.addInstruction(0, "return-object p0")
        AppsFlyerStartFingerprint.matchAll(2..2).forEach { it.method.returnEarly() }
        FlurrySupportedFingerprint.method.returnEarly(false)
        FlurryContentProviderCreateFingerprint.method.returnEarly(true)
        ComScoreStartFingerprint.method.returnEarly()
    }
}
