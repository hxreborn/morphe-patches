/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wattpad.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val UNBOX_ADS_FREE_HOUR_WINDOW = 3

internal object AdsFreeHourOverrideFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    filters = listOf(
        methodCall(definingClass = "Lkotlinx/coroutines/flow/StateFlow;", name = "getValue"),
        methodCall(
            definingClass = "Ljava/lang/Boolean;",
            name = "booleanValue",
            location = MatchAfterWithin(UNBOX_ADS_FREE_HOUR_WINDOW),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/List;", name = "contains"),
    ),
)
