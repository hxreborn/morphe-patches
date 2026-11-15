/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.layout.insets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object ActivityContentFingerprint : Fingerprint(
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("onCreate()"),
    filters = listOf(
        methodCall(name = "setContentView", parameters = listOf("I"), returnType = "V"),
    ),
)

internal object AudioActivityFingerprint : Fingerprint(
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        fieldAccess(name = "activity_track_detail", opcode = Opcode.SGET),
        methodCall(name = "setContentView", parameters = listOf("I"), returnType = "V"),
    ),
)

internal object QueueLayoutFingerprint : Fingerprint(
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(fieldAccess(name = "dialog_music_queue", opcode = Opcode.SGET)),
)

internal fun queueWindowFingerprint(owner: String) = Fingerprint(
    definingClass = owner,
    filters = listOf(methodCall(definingClass = "Landroid/view/Window;", name = "setLayout")),
)
