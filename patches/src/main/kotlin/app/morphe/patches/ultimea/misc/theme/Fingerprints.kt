/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.methodCall
import app.morphe.patcher.InstructionLocation

internal object ActivityCreateFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(string("onCreate: checkGooglePlayServices:")),
)

internal object SystemBarStyleFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(string("statusBarIconBrightness"), string("systemNavigationBarIconBrightness")),
)

internal object ImageDecoderFingerprint : Fingerprint(
    definingClass = "Lio/flutter/embedding/engine/FlutterJNI;",
    name = "decodeImage",
    returnType = "Landroid/graphics/Bitmap;",
    parameters = listOf("Ljava/nio/ByteBuffer;", "J"),
    filters = listOf(methodCall(returnType = "Landroid/graphics/Bitmap;")),
)

internal object PlatformBrightnessFingerprint : Fingerprint(
    filters = listOf(
        string("platformBrightness"),
        methodCall(definingClass = "Ljava/util/HashMap;", name = "put", location = InstructionLocation.MatchAfterImmediately()),
    ),
)

internal object ImageHeaderCallbackFingerprint : Fingerprint(
    definingClass = "Lio/flutter/embedding/engine/FlutterJNI;",
    name = "nativeImageHeaderCallback",
    returnType = "V",
    parameters = listOf("J", "I", "I"),
    isStatic = true,
)
