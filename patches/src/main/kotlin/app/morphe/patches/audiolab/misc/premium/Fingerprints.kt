/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiolab.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object ProUserCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Lcom/hitrolab/ffmpeg/Keys;", name = "nativeGetCode"),
    ),
)

internal object AppSignatureHashFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    strings = listOf("  ||||  ", "sha256"),
)

internal object FreeProductPriceFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("\$0.00"),
)
