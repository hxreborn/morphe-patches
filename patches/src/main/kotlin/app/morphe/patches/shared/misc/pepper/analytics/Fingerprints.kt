/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.analytics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

internal const val ANALYTICS_REPORT_ANNOTATION_PREFIX = "analytics-event-report"

internal object AdjustInitFingerprint : Fingerprint(
    definingClass = "Lcom/adjust/sdk/Adjust;",
    name = "initSdk",
    returnType = "V",
    parameters = listOf("Lcom/adjust/sdk/AdjustConfig;"),
)

internal object WorkSuccessFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Success {mOutputData="),
)

internal object AnalyticsReportFingerprint : Fingerprint(
    custom = { method, _ ->
        method.annotations.any { annotation ->
            annotation.elements.any { element ->
                (element.value as? StringEncodedValue)?.value?.startsWith(ANALYTICS_REPORT_ANNOTATION_PREFIX) == true
            }
        }
    },
)

internal fun analyticsWorkerFingerprint(workerType: String, successType: String) = Fingerprint(
    definingClass = workerType,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
    filters = listOf(newInstance(successType)),
)

internal fun deprecatedAnalyticsWorkerFingerprint(successType: String) = Fingerprint(
    definingClass = "Lcom/pepper/analytics/deprecated/DeprecatedAnalyticsEventTransmissionWorker;",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
    strings = listOf(
        "ocular_event_hash",
        "ocular_event_time_in_seconds",
        "Analytics event has been transmitted: ",
    ),
    filters = listOf(newInstance(successType)),
)
