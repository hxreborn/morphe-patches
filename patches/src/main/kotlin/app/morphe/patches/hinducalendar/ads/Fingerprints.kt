/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hinducalendar.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val MAIN_ACTIVITY_CLASS = "Lcom/alokmandavgane/hinducalendar/MainActivity;"

internal object OnQueryPurchasesResponseFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("In onQueryPurchasesResponse"),
        methodCall(definingClass = "Ljava/util/Collection;", name = "isEmpty"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        string("Purchase Found. Not showing ads"),
        fieldAccess(
            definingClass = MAIN_ACTIVITY_CLASS,
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN,
            location = MatchAfterWithin(5),
        ),
    ),
)

internal object MainActivityConstructorFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "<init>",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(opcode(Opcode.RETURN_VOID)),
)
