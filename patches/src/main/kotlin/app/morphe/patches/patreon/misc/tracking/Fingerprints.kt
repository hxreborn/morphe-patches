/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val APPS_FLYER_LIB = "Lcom/appsflyer/AppsFlyerLib;"

internal object AmplitudeLogEventFingerprint : Fingerprint(
    returnType = "J",
    filters = listOf(string("Logged event to Amplitude: ")),
)

internal object AppsFlyerInitFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(methodCall(definingClass = APPS_FLYER_LIB, name = "init")),
    custom = { _, classDef -> classDef.sourceFile == "AppsFlyerImpl.kt" },
)

internal object AppsFlyerStartFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(methodCall(definingClass = APPS_FLYER_LIB, name = "start")),
    custom = { _, classDef -> classDef.sourceFile == "AppsFlyerImpl.kt" },
)

internal object SentryOptionsFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(methodCall(name = "setDsn", parameters = listOf("Ljava/lang/String;"))),
    custom = { _, classDef -> classDef.sourceFile == "SentryUtilImpl.kt" },
)

internal object SiftOpenFingerprint : Fingerprint(
    definingClass = "Lsiftscience/android/Sift;",
    name = "open",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Lsiftscience/android/Sift\$Config;", "Ljava/lang/String;"),
)

internal object SiftCollectFingerprint : Fingerprint(
    definingClass = "Lsiftscience/android/Sift;",
    name = "collect",
    returnType = "V",
    parameters = listOf(),
)

internal object MuxStatsTrackingFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/patreon/android/data/model/PatreonMediaItem;"),
    filters = listOf(string("Mux stats operations are happening after release of player.")),
)
