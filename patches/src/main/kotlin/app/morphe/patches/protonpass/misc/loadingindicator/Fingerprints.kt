/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.misc.proton.pullRefreshContentFingerprint
import app.morphe.patches.shared.misc.proton.sizeModifierCall
import com.android.tools.smali.dexlib2.Opcode

internal const val PULL_REFRESH_STATE = "Landroidx/compose/material/pullrefresh/PullRefreshState;"

internal val PullRefreshContentFingerprint = pullRefreshContentFingerprint(
    "Landroidx/compose/material/ProgressIndicatorKt;",
    "Landroidx/compose/material/pullrefresh/PullRefreshIndicatorKt;",
)

internal object PullRefreshProgressFingerprint : Fingerprint(
    filters = listOf(
        methodCall(definingClass = PULL_REFRESH_STATE, parameters = emptyList(), returnType = "F"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(definingClass = PULL_REFRESH_STATE, opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(parameters = emptyList(), returnType = "F", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.DIV_FLOAT_2ADDR, MatchAfterWithin(0)),
    ),
)

internal object PullRefreshIndicatorSizeFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Landroidx/compose/material/pullrefresh/PullRefreshIndicatorKt;",
            name = "IndicatorSize",
            opcode = Opcode.SGET,
        ),
        sizeModifierCall(MatchAfterImmediately()),
    ),
)
