/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import app.morphe.patches.uptcl.misc.fix.signature.trustInstalledCertificatePatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Firebase Sessions, Facebook, TikTok and the app's own " +
        "event reporting from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.UPTCL)
    dependsOn(
        trustInstalledCertificatePatch,
        disableAnalyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
    )

    execute {
        LogEventsToLibraryFingerprint.matchSingle().classDef.methods
            .filter { method ->
                method.returnType == "V" &&
                    AccessFlags.PUBLIC.isSet(method.accessFlags) &&
                    !AccessFlags.STATIC.isSet(method.accessFlags)
            }
            .forEach { it.returnEarly() }
    }
}
