/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.periodcalendar.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val PRODUCT_CLASS = "Lcom/digitalchemy/foundation/applicationmanagement/market/Product;"

internal object IsProFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(string("Pro")),
)

internal object IsAdFreeFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(string("Remove ads")),
)

internal object WorkbookPurchasedFingerprint : Fingerprint(
    strings = listOf("isWorkbookPurchased"),
    filters = listOf(
        methodCall(
            parameters = listOf(PRODUCT_CLASS),
            returnType = "Z",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
