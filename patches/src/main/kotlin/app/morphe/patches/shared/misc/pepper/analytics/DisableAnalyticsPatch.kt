/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch

private const val BLOCKED_ANALYTICS_REPORT_URL = "http://127.0.0.1:1/pepper-analytics"
private const val EXPECTED_ANALYTICS_REPORT_COUNT = 4

private val analyticsWorkerTypes = listOf(
    "Lcom/pepper/analytics/backgroundjob/AnalyticsEventTransmissionWorker;",
    "Lcom/pepper/analytics/backgroundjob/ExternalTrackingPixelTransmissionWorker;",
    "Lcom/pepper/analytics/backgroundjob/RecombeeEventTransmissionWorker;",
    "Lcom/pepper/apps/android/backgroundjob/worker/LeafletTrackingTransmissionWorker;",
)

@Suppress("unused")
val disableAnalyticsPatch = patch(
    name = "Disable analytics",
    description = "Stops usage and interaction reports.",
) {
    compatibleWith(AppCompatibilities.CHOLLOMETRO, AppCompatibilities.PEPPER)

    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch)

    execute {
        AdjustInitFingerprint.method.returnEarly()

        val successType = WorkSuccessFingerprint.match().classDef.type
        val analyticsWorkerMethods = analyticsWorkerTypes.map { analyticsWorkerFingerprint(it, successType).match().method } +
            deprecatedAnalyticsWorkerFingerprint(successType).match().method
        analyticsWorkerMethods.forEach { method ->
            method.addInstructions(
                0,
                """
                    new-instance v0, $successType
                    invoke-direct { v0 }, $successType-><init>()V
                    return-object v0
                """,
            )
        }

        AnalyticsReportFingerprint.matchAll(
            EXPECTED_ANALYTICS_REPORT_COUNT..EXPECTED_ANALYTICS_REPORT_COUNT,
        ).forEach { match ->
            val annotationValue = match.method.annotations
                .flatMap { it.elements }
                .mapNotNull { it.value as? MutableStringEncodedValue }
                .single { it.value.startsWith(ANALYTICS_REPORT_ANNOTATION_PREFIX) }
            annotationValue.setValue(BLOCKED_ANALYTICS_REPORT_URL)
        }
    }
}
