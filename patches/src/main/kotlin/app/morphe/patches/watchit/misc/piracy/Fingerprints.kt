/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.piracy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterLast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object PiracyCheckerStartFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    strings = listOf("\$this\$verifySigningCertificate", "\$this\$verifyInstallerId"),
    filters = listOf(fieldAccess(opcode = Opcode.IGET_OBJECT)),
)

internal fun allowVerificationCallbackFingerprint(callbackType: String) = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = callbackType,
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = emptyList(),
            location = MatchAfterLast(),
        ),
    ),
)
