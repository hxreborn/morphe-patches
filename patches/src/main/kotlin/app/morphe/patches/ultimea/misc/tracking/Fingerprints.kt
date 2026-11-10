/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.tracking

import app.morphe.patcher.Fingerprint

internal object AutomaticCrashlyticsCollectionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/internal/common/DataCollectionArbiter;",
    name = "isAutomaticDataCollectionEnabled",
    parameters = emptyList(),
    returnType = "Z",
)

internal object CrashlyticsBooleanCollectionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "setCrashlyticsCollectionEnabled",
    parameters = listOf("Z"),
    returnType = "V",
)

internal object CrashlyticsBoxedCollectionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "setCrashlyticsCollectionEnabled",
    parameters = listOf("Ljava/lang/Boolean;"),
    returnType = "V",
)

internal object CrashlyticsSendReportsFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "sendUnsentReports",
    parameters = emptyList(),
    returnType = "V",
)

internal object CrashlyticsLogFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "log",
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
)

internal object CrashlyticsExceptionFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "recordException",
    parameters = listOf("Ljava/lang/Throwable;"),
    returnType = "V",
)

