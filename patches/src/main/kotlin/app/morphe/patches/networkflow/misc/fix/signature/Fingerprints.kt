/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.fix.signature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private fun archiveSignatureListFingerprint(
    definingClass: String? = null,
    methodName: String? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
) = Fingerprint(
    definingClass = definingClass,
    name = methodName,
    returnType = returnType,
    parameters = parameters,
    filters = listOf(
        opcode(Opcode.FILLED_NEW_ARRAY),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "Ljava/util/ArrayList;", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "Ljava/util/ArrayList;",
            name = "isEmpty",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal val mainActivityArchiveSignatureFingerprint =
    archiveSignatureListFingerprint("Lcom/evozi/network/view/MainActivity;", "onCreate")

internal val networkServiceArchiveSignatureFingerprint =
    archiveSignatureListFingerprint("Lcom/evozi/network/service/NetworkService;", "onCreate")

internal val appFunctionArchiveSignatureFingerprint =
    archiveSignatureListFingerprint(returnType = "Z", parameters = emptyList())
