/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object LessonAccessFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("REQUIRE_LOGIN"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun signInPromptFingerprint(requireLogin: FieldReference) = Fingerprint(
    name = "invokeSuspend",
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.SGET_BOOLEAN),
        fieldAccess(
            definingClass = "Ljava/lang/Boolean;",
            name = "FALSE",
            opcode = Opcode.SGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(requireLogin, Opcode.SGET_OBJECT),
    ),
)
