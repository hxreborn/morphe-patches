/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.analytics

import app.morphe.patcher.Fingerprint

internal object SetCrashlyticsCollectionEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;",
    name = "setCrashlyticsCollectionEnabled",
    returnType = "V",
    parameters = listOf("Z"),
)

internal object SingularInitFingerprint : Fingerprint(
    definingClass = "Lcom/singular/sdk/Singular;",
    name = "init",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Lcom/singular/sdk/SingularConfig;"),
)

internal object SurvicateInitFingerprint : Fingerprint(
    definingClass = "Lcom/survicate/surveys/Survicate;",
    name = "init",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Z"),
)
