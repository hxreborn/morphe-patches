/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.rate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object RatingDialogFingerprint : Fingerprint(
    strings = listOf("rate_popup_show", "rate_popup_show_from_result"),
    filters = listOf(
        string("rateFirst"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)
