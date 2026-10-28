/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.deleted

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val DELETE_WINDOW_MILLIS = 3_600_000L

internal object DeleteMessageHandlerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("delete_message:"),
        methodCall(definingClass = "Ljava/lang/String;", name = "replace", location = MatchAfterWithin(3)),
        methodCall(name = "findMessageWithStanzaId", location = MatchAfterWithin(3)),
        literal(DELETE_WINDOW_MILLIS, location = MatchAfterWithin(30)),
        opcode(Opcode.IF_GTZ, location = MatchAfterWithin(2)),
    ),
)
