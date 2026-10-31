/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.tracking

import app.morphe.patcher.Fingerprint

private val appsFlyerReportingMethods = setOf("start", "logEvent", "logSession", "logAdRevenue", "logLocation")

internal object AppsFlyerInitFingerprint : Fingerprint(
    name = "init",
    returnType = "Lcom/appsflyer/AppsFlyerLib;",
    custom = { _, classDef -> classDef.superclass == "Lcom/appsflyer/AppsFlyerLib;" },
)

internal object AppsFlyerReportingFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        classDef.superclass == "Lcom/appsflyer/AppsFlyerLib;" && method.name in appsFlyerReportingMethods
    },
)

internal object MParticleConfigRequestFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("Config request attempt:\nURL- "),
)

internal object MParticleUploadFingerprint : Fingerprint(
    returnType = "I",
    strings = listOf("Upload request attempt:\nURL- "),
)

internal object NpawAnalyticsEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/npaw/core/options/AnalyticsOptions;",
    name = "isEnabled",
    returnType = "Z",
    parameters = emptyList(),
)

internal object NpawEventSendFingerprint : Fingerprint(
    definingClass = "Lcom/npaw/core/consumers/nqs/NqsAnalyticsService;",
    name = "send",
    returnType = "V",
)

internal object CleverTapQueueFlushFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Network connectivity unavailable. Will retry later"),
)
