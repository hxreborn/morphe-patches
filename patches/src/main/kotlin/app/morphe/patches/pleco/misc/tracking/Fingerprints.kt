/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ServerRequestDeviceFieldsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L"),
    strings = listOf("plecoversion", "anid", "bserial"),
    filters = listOf(
        string("locale"),
        methodCall(definingClass = "Landroid/content/Context;", name = "getContentResolver"),
    ),
)

internal object TransportEventScheduleFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Error scheduling event "),
    filters = listOf(
        string("Transport backend '"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)
