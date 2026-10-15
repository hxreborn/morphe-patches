/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags

internal object LinkCodecExecutableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/io/File;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/io/File;", name = "exists"),
        methodCall(definingClass = "Ljava/io/File;", name = "mkdirs"),
        newInstance("Lorg/readera/codec/exception/OreInstallException;"),
    ),
)
