/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Flurry, comScore, Firebase Analytics and Facebook from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    dependsOn(disableAnalyticsCollectionPatch)

    execute {
        AppsFlyerInitFingerprint.matchSingle().method.addInstruction(0, "return-object p0")
        AppsFlyerStartFingerprint.matchAll().forEach { it.method.returnEarly() }
        FlurrySupportedFingerprint.matchSingle().method.returnEarly(false)
        FlurryContentProviderCreateFingerprint.matchSingle().method.returnEarly(true)
        ComScoreStartFingerprint.matchSingle().method.returnEarly()
    }
}
