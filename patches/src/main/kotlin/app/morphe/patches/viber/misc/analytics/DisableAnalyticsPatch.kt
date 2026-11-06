/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.analytics

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FIREBASE_ANALYTICS_KEY = "firebase_analytics_collection_enabled"
private const val ADID_COLLECTION_KEY = "google_analytics_adid_collection_enabled"
private const val COLLECTION_FILTER = "$VIBER_SETTINGS_CLASS->analyticsCollectionEnabled(Ljava/lang/Boolean;)Ljava/lang/Boolean;"
private val ADID_COLLECTION_SITES = 2..2

@Suppress("unused")
val disableAnalyticsPatch: BytecodePatch = bytecodePatch(
    name = "Disable analytics",
    description = "Stops Mixpanel, Adjust, Braze, Firebase, Crashlytics and Snap Kit from collecting and sending usage data.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("disableAnalytics", disableAnalyticsPatch)
        TrackerAllowedFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        SnapKitInitFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        FirebaseSessionsEnabledFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        CrashlyticsEnabledFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        TrackerConsentFingerprint.matchSingle().apply {
            val (setEnabled, _, _) = instructionMatches
            val reference = setEnabled.instruction.getReference<MethodReference>()!!
            val trackerRegister = method.implementation!!.registerCount - 1
            method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z") { register -> """
                const/16 v$register, 0x0
                invoke-interface { v$trackerRegister, v$register }, $reference
                return-void
            """ }
        }
        val metadataReader = FirebaseMetadataFingerprint.matchSingle().method
        val collectionFlags = listOf(metadataFlagFingerprint(FIREBASE_ANALYTICS_KEY, metadataReader).matchSingle()) +
            metadataFlagFingerprint(ADID_COLLECTION_KEY, metadataReader).matchAll(ADID_COLLECTION_SITES)
        collectionFlags.forEach { match ->
            val (_, _, result) = match.instructionMatches
            match.method.addFilterCallAfter(result.index, COLLECTION_FILTER)
        }
    }
}
