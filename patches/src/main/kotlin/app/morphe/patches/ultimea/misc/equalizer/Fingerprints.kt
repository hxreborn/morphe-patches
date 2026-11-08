/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.equalizer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal class ReadbackCallFingerprint(name: String, parameters: List<String>? = null) : Fingerprint(
    filters = listOf(methodCall(
        definingClass = "Lapp/hxreborn/extension/ultimea/controls/XupmixReadback;",
        name = name,
        parameters = parameters,
    )),
    custom = { _, classDef -> !classDef.type.startsWith("Lapp/hxreborn/extension/") },
)

internal object DeviceActivityFingerprint : Fingerprint(
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    filters = listOf(methodCall(
        definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
        name = "isGooglePlayServicesAvailable",
        parameters = listOf("Landroid/content/Context;"),
        returnType = "I",
    )),
)

internal object NavigationMessageFingerprint : Fingerprint(
    definingClass = "Lio/flutter/embedding/engine/FlutterJNI;",
    name = "handlePlatformMessage",
    parameters = listOf("Ljava/lang/String;", "Ljava/nio/ByteBuffer;", "I", "J"),
    returnType = "V",
)
