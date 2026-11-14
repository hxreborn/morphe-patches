/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.requireStandardInstall

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Braze and Firebase Analytics data collection and blocks King's reads of " +
        "the advertising ID and install referrer. Braze push and in-app messages stop too. " +
        "Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    availability(requireStandardInstall)
    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch, disableTrackingManifestEntriesPatch)

    execute {
        if (classDefByOrNull(APPSFLYER_LIB) != null || classDefByOrNull(APPSFLYER_MODULE) != null) {
            val initialization = AppsFlyerInitializeFingerprint.matchSingle()
            val (firstSdkAccess) = initialization.instructionMatches
            initialization.method.addInstruction(firstSdkAccess.index, "return-void")
            AppsFlyerStartFingerprint.matchSingle().method.returnEarly()
            AppsFlyerCustomerUserIdFingerprint.matchSingle().method.returnEarly()
        }
        AdvertisingIdLoadFingerprint.matchSingle().method.returnEarly(null)
        TrackingEntryFingerprints.forEach { it.matchSingle().method.returnEarly() }
        BrazeModuleVoidEntryFingerprints.forEach { it.matchSingle().method.returnEarly() }
        BrazeIdFromFeatureFlagFingerprint.matchSingle().method.returnEarly("")
    }
}
