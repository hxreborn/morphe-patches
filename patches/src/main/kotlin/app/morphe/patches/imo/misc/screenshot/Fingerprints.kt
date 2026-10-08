/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.screenshot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal object AddWindowFlagsFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/",
    filters = listOf(methodCall(definingClass = "Landroid/view/Window;", name = "addFlags")),
)

internal object SetWindowFlagsFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/",
    filters = listOf(methodCall(definingClass = "Landroid/view/Window;", name = "setFlags")),
)

internal object RegisterCaptureCallbacksFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/view/Window;", "L", "L", "Ljava/lang/String;"),
    strings = listOf("registerScreenCaptureCallback success key="),
)

internal object DispatchScreenshotFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("same path, ignore "),
)

internal object RegisterCaptureObserverFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(string("register observer")),
)

internal object RegisterImageCaptureObserverFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L", "Z"),
    strings = listOf("register image observer"),
)

internal object RegisterCallCaptureObserverFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(string("register observer"), string("CallDetectCaptureScreenUtils")),
)
