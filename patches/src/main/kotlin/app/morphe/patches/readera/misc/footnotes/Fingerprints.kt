/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.footnotes

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FootnoteUtteranceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/List;", name = "get"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, MatchAfterImmediately()),
        opcode(Opcode.IGET, MatchAfterImmediately()),
        literal(0x16, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, MatchAfterImmediately()),
    ),
)

internal fun skipFootnoteFingerprint(utteranceClass: String, isFootnote: String) = Fingerprint(
    definingClass = utteranceClass,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = utteranceClass, name = isFootnote),
    ),
)

internal object SpeakFootnoteFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("ReadTextPlayer speak note num:[%s], text:[%s]"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_DIRECT, returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_DIRECT, returnType = "Ljava/lang/String;", location = MatchAfterImmediately()),
        opcode(Opcode.SGET_BOOLEAN, MatchAfterImmediately()),
    ),
)
