/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.hostdetails

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.netanalyzer.shared.PING_RESULT_ROW
import app.morphe.patches.netanalyzer.shared.composable
import app.morphe.patches.netanalyzer.shared.decryptedString
import com.android.tools.smali.dexlib2.Opcode

private const val OBJECT_TYPE_PREFIX = "L"
private const val COLOR_TYPE = "J"

internal object PingResultRowBackgroundFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        composable(PING_RESULT_ROW),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(OBJECT_TYPE_PREFIX, COLOR_TYPE, OBJECT_TYPE_PREFIX),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

private const val COUNTRY_FLAG = "Country flag"
private const val WIDTH_TYPE = "F"

internal object PingResultRowSpacerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        composable(PING_RESULT_ROW),
        decryptedString(COUNTRY_FLAG),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(OBJECT_TYPE_PREFIX, WIDTH_TYPE),
            returnType = OBJECT_TYPE_PREFIX,
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(OBJECT_TYPE_PREFIX, OBJECT_TYPE_PREFIX),
            returnType = "V",
        ),
    ),
)
