/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.background

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object SpeechPingFingerprint : Fingerprint(
    definingClass = "Lorg/readera/SpeechService;",
    name = "onEventMainThread",
    strings = listOf("SpeechService EventTextToSpeechPlay ping:%d"),
    filters = listOf(
        literal(3),
        opcode(Opcode.IF_LE, MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        opcode(Opcode.INVOKE_DIRECT, MatchAfterImmediately()),
        opcode(Opcode.INVOKE_STATIC, MatchAfterImmediately()),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately()),
    ),
)

internal object SpeechModelFingerprint : Fingerprint(
    strings = listOf("SpeechModel setPlaying %b"),
)

internal fun readerStopFingerprint(speechModelClass: String) = Fingerprint(
    definingClass = "Lorg/readera/read/ReadActivity;",
    name = "onStop",
    filters = listOf(
        methodCall(definingClass = speechModelClass, opcode = Opcode.INVOKE_STATIC),
    ),
)
