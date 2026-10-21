/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.tracking

import app.morphe.patcher.Fingerprint

internal object CrashlyticsSetCollectionEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "setCrashlyticsCollectionEnabled",
    returnType = "V",
    parameters = listOf("Z"),
)

internal object KlaviyoInitializeFingerprint : Fingerprint(
    definingClass = "Lcom/klaviyo/analytics/Klaviyo;",
    name = "initialize",
    returnType = "Lcom/klaviyo/analytics/Klaviyo;",
    parameters = listOf("Ljava/lang/String;", "Landroid/content/Context;"),
)

internal object KochavaStartFingerprint : Fingerprint(
    definingClass = "Lcom/kochava/reactlibrary/RNKochavaTrackerModule;",
    name = "start",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
)
