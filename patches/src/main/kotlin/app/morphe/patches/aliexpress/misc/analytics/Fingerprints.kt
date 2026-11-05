/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/analytics/DisableAnalyticsPatch.kt
 */
package app.morphe.patches.aliexpress.misc.analytics

import app.morphe.patcher.Fingerprint

internal object UtSendFingerprint : Fingerprint(
    definingClass = "Lcom/ut/mini/UTTracker;",
    name = "send",
    parameters = listOf("Ljava/util/Map;"),
    returnType = "V",
)

internal object FirebaseInstanceFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "getInstance",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
)

internal object FirebaseCollectionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "setAnalyticsCollectionEnabled",
    parameters = listOf("Z"),
    returnType = "V",
)

internal object AffiliateUrlTrackingFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("aff_platform", "sendDataToAff exception "),
)

internal object AffiliateLongUrlTrackingFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("TrafficServiceImpl_doReportAffPlatformLongUrl"),
)
