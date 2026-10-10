/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.sqcom.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val FORM_ERROR_TYPE = "Lcom/google/android/ump/FormError;"

internal object ConsentRequestFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/ump/ConsentInformation;",
            name = "requestConsentInfoUpdate",
        ),
    ),
)

internal object AdListResponseFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("RequestAdListResult"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = emptyList(),
            returnType = "Ljava/util/List;",
            location = MatchAfterWithin(3),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun consentListenerFingerprint(listenerType: String) = Fingerprint(
    definingClass = listenerType,
    returnType = "V",
    parameters = listOf(FORM_ERROR_TYPE),
)
