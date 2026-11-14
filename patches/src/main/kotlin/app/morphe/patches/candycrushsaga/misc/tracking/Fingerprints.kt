/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object AppsFlyerInitializeFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/appsflyer/AppsFlyerModule;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "J", "J", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/appsflyer/AppsFlyerLib;",
            name = "getInstance",
            returnType = "Lcom/appsflyer/AppsFlyerLib;",
            parameters = emptyList(),
        ),
    ),
)

internal object AppsFlyerCustomerUserIdFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/appsflyer/AppsFlyerModule;",
    name = "setCustomerUserId",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object AppsFlyerStartFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/appsflyer/AppsFlyerModule;",
    name = "start",
    returnType = "V",
    parameters = emptyList(),
)

internal object AdvertisingIdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/king/abm/internal/google/GoogleAdvertisingIdSource\$LoadTask;",
    name = "doInBackground",
    returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;",
    parameters = listOf("[Ljava/lang/Void;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient;",
            name = "getAdvertisingIdInfo",
            returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;",
            parameters = listOf("Landroid/content/Context;"),
        ),
    ),
)

internal val TrackingEntryFingerprints = listOf(
    Fingerprint(
        definingClass = "Lcom/king/firebaseanalytics/FirebaseAnalyticsManager;",
        name = "logEvent",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lcom/king/firebaseanalytics/FirebaseAnalyticsManager;",
        name = "setUserProperty",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lcom/king/usdk/adinfo/AdvertisingId\$AdvertisingIdInitializer;",
        name = "run",
        returnType = "V",
        parameters = emptyList(),
    ),
    Fingerprint(
        definingClass = "Lcom/king/usdk/adinfo/ReferrerProvider;",
        name = "startFetch",
        returnType = "V",
    ),
)

private val brazeModuleVoidEntries = listOf(
    "configure",
    "requestImmediateDataFlush",
    "setCustomUserAttribute",
    "setMultipleCustomUserAttributes",
    "setCustomUserAttributeToNow",
    "setCustomUserAttributeToSecondsFromEpoch",
    "unsetCustomUserAttribute",
    "setExternalUserId",
)

internal val BrazeModuleVoidEntryFingerprints = brazeModuleVoidEntries.map { name ->
    Fingerprint(
        definingClass = "Lcom/king/usdk/braze/BrazeAndroidModule;",
        name = name,
        returnType = "V",
    )
}

internal object BrazeIdFromFeatureFlagFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/braze/BrazeAndroidModule;",
    name = "getBrazeIdFromFeatureFlag",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)
