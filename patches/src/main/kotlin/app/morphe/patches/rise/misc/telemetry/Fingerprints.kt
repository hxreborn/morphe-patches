/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.telemetry

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags

internal object SentryInitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L", "L"),
    strings = listOf("Fatal error during SentryAndroid.init(...)"),
)

internal object UsageEventToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("ApplicationUsageEvent [usageDate="),
)

internal fun usageEventMapFingerprint(eventType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("Ljava/util/List;"),
    filters = listOf(newInstance(eventType)),
)
