/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.shared.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val EMPTY_SIGNAL_CHECK_WINDOW = 4
private const val EMAIL_LOGIN_REQUEST_CLASS = "Lcom/duolingo/data/signuplogin/LoginRequest\$EmailLoginRequest;"

internal object EmailLoginRequestSerializeFingerprint : Fingerprint(
    name = "serialize",
    returnType = "V",
    parameters = listOf("Lkotlinx/serialization/encoding/Encoder;", "Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(definingClass = EMAIL_LOGIN_REQUEST_CLASS, opcode = Opcode.IGET_OBJECT),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
        instanceOf("Lcom/duolingo/core/integrity/SecuritySignal\$PlayIntegrityToken;", MatchAfterWithin(EMPTY_SIGNAL_CHECK_WINDOW)),
    ),
)
