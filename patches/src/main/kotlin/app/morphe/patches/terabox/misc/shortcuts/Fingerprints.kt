/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.shortcuts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun shortcutConfigFingerprint(configKey: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string(configKey),
        methodCall(parameters = listOf("Ljava/lang/String;"), returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)
