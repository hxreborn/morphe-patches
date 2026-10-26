/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.tracking

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AnalyticsTrackerFingerprint : Fingerprint(
    definingClass = "Lcom/athan/model/FireBaseAnalyticsTrackers;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    custom = { method, _ ->
        method.name.startsWith("track") || method.name == "setUserProperty" || method.name == "setDefaults"
    },
)

internal object FacebookActivateAppFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/appevents/AppEventsLogger\$Companion;",
    name = "activateApp",
    returnType = "V",
    parameters = listOf("Landroid/app/Application;", "Ljava/lang/String;"),
)
