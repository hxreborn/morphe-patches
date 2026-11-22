/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.requireStandardInstall

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Braze, Firebase Analytics and Google measurement from collecting data, " +
        "and removes the advertising ID and install referrer permissions. " +
        "Braze push and in-app messages stop too.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    availability(requireStandardInstall)
    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch, disableTrackingManifestEntriesPatch)

    execute {
        if (classDefByOrNull(APPSFLYER_LIB) != null || classDefByOrNull(APPSFLYER_MODULE) != null) {
            val initialization = AppsFlyerInitializeFingerprint.match()
            val (firstSdkAccess) = initialization.instructionMatches
            initialization.method.addInstruction(firstSdkAccess.index, "return-void")
            AppsFlyerStartFingerprint.method.returnEarly()
            AppsFlyerCustomerUserIdFingerprint.method.returnEarly()
        }
        AdvertisingIdLoadFingerprint.method.returnEarly(null)
        TrackingEntryFingerprints.forEach { it.method.returnEarly() }
        BrazeModuleVoidEntryFingerprints.forEach { it.method.returnEarly() }
        BrazeIdFromFeatureFlagFingerprint.method.returnEarly("")
    }
}
