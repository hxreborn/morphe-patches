/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.thumbnails

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val BOOLEAN_CLASS = "Ljava/lang/Boolean;"
private const val APPLICATION_CLASS = "Lcom/cardfeed/video_public/application/MainApplication;"

internal object PlayerLoadFingerprint : Fingerprint(
    isStatic = false,
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z", "Z", "Z"),
    strings = listOf("youtube.com", ".m3u8", ".mpd"),
)

internal object PlayerTogglePlaybackFingerprint : Fingerprint(
    classFingerprint = PlayerLoadFingerprint,
    isStatic = false,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(name = "pause", parameters = listOf(), returnType = "V"),
        fieldAccess(definingClass = BOOLEAN_CLASS, name = "TRUE", opcode = Opcode.SGET_OBJECT),
        fieldAccess(definingClass = BOOLEAN_CLASS, name = "FALSE", opcode = Opcode.SGET_OBJECT),
        methodCall(definingClass = "this", parameters = listOf(), returnType = "Z"),
    ),
)

internal object AutoplayAllowedFingerprint : Fingerprint(
    classFingerprint = PlayerLoadFingerprint,
    isStatic = true,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(fieldAccess(definingClass = APPLICATION_CLASS, opcode = Opcode.SGET_OBJECT)),
)
