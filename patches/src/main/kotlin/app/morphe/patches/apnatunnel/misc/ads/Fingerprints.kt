/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.apnatunnel.misc.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AdsSdkCoreFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf("initializationTasks", "sdkCoreEngine"),
)

internal object AdLoadFingerprint : Fingerprint(
    classFingerprint = AdsSdkCoreFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
)

internal object ConsentInfoUpdateFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(\"",
        "RequestConsentUpdate RuntimeException. ",
    ),
)
