/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.Fingerprint

internal object LoganBindingFingerprint : Fingerprint(strings = listOf("flutter_logan"))

internal object LoganMethodCallFingerprint : Fingerprint(
    classFingerprint = LoganBindingFingerprint,
    name = "onMethodCall",
    returnType = "V",
)

internal object FlutterInitFingerprint : Fingerprint(
    definingClass = "Lio/flutter/embedding/engine/FlutterJNI;",
    name = "init",
    parameters = listOf("Landroid/content/Context;", "[Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "J", "I"),
    returnType = "V",
)
