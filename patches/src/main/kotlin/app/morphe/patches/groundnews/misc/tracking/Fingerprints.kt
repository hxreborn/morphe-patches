/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.tracking

import app.morphe.patcher.Fingerprint

internal val TelemetryFingerprints = listOf(
    Fingerprint(
        definingClass = "Lvc/ucic/logger/AfStartupHelper;",
        name = "onAfSdkReadyToStart",
        returnType = "V",
        parameters = emptyList(),
    ),
    Fingerprint(
        definingClass = "Lcom/ground/multiplatform/ui/posthog/GroundPostHog;",
        name = "trackEvent",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lcom/ground/multiplatform/ui/posthog/GroundPostHog;",
        name = "identify",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lvc/ucic/logger/AmplitudeHelper;",
        name = "initAmplitude",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lvc/ucic/logger/AmplitudeHelper;",
        name = "updateUserInfo",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lvc/ucic/tracking/SmartLookTrackingImpl;",
        name = "startRecording",
        returnType = "V",
    ),
    Fingerprint(
        definingClass = "Lvc/ucic/logger/AndroidLogger;",
        name = "logAppsFlyerEvent",
        returnType = "V",
    ),
)

internal object AmplitudeEventFingerprint : Fingerprint(
    definingClass = "Lvc/ucic/logger/AmplitudeHelper;",
    name = "logEvent",
    returnType = "V",
)

internal val AnalyticsOptOutFingerprints = listOf(
    Fingerprint(
        definingClass = "Lcom/posthog/PostHogConfig;",
        name = "getOptOut",
        returnType = "Z",
        parameters = emptyList(),
    ),
    Fingerprint(
        definingClass = "Lcom/amplitude/core/Configuration;",
        name = "getOptOut",
        returnType = "Z",
        parameters = emptyList(),
    ),
    Fingerprint(
        definingClass = "Lcom/amplitude/android/Configuration;",
        name = "getOptOut",
        returnType = "Z",
        parameters = emptyList(),
    ),
)
