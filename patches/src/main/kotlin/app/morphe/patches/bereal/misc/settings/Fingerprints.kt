/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object DebugScreenActionToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("DebugScreen"),
)

internal object AboutSectionRowsFingerprint : Fingerprint(
    name = "invokeSuspend",
    filters = listOf(
        string("settingsscreen_about_button"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC_RANGE,
            parameters = listOf("I", "Ljava/lang/String;", "L", "Z", "Ljava/lang/String;", "Ljava/lang/String;"),
            location = MatchAfterWithin(4),
        ),
        literal(0, opcodes = listOf(Opcode.CONST_4), location = MatchAfterWithin(5)),
        opcode(Opcode.FILLED_NEW_ARRAY, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun settingsActionClickFingerprint(debugScreenAction: String) = Fingerprint(
    filters = listOf(
        fieldAccess(definingClass = debugScreenAction, type = debugScreenAction, opcode = Opcode.SGET_OBJECT),
        methodCall(returnType = "Z", location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
    ),
)
