/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.muvizwidgets.misc.weather

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val WEATHER_API_BASE_URL = "https://api.sparkine.com/weather/widgets/"

internal object WeatherApiClientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf(WEATHER_API_BASE_URL),
)

internal object AppCheckExchangeFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("Unable to get heartbeats!"),
)

internal object AppCheckProviderInstallFingerprint : Fingerprint(
    definingClass = "Lcom/sparkine/widgets/MobileApp;",
    name = "onCreate",
)

internal object DebugAppCheckRegistrarFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/appcheck/debug/FirebaseAppCheckDebugRegistrar;",
    name = "getComponents",
)
