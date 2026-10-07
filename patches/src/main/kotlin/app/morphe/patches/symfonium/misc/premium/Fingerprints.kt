/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.symfonium.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object LicenseKeyCheckFingerprint : Fingerprint(
    strings = listOf("S2V5Q2hlY2s="),
)

internal object IsLicensedFingerprint : Fingerprint(
    classFingerprint = LicenseKeyCheckFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        anyInstruction(
            opcode(Opcode.CONST_WIDE_16),
            opcode(Opcode.CONST_WIDE_32),
            opcode(Opcode.CONST_WIDE),
        ),
        opcode(Opcode.CMP_LONG, MatchAfterImmediately()),
    ),
)

internal object LicenseStateWriterFingerprint : Fingerprint(
    classFingerprint = LicenseKeyCheckFingerprint,
    returnType = "V",
    parameters = listOf("J"),
)

internal object NativeVerdictHandlerFingerprint : Fingerprint(
    definingClass = "Lapp/symfonik/init/HandlerInitializable\$handler\$1;",
    name = "init",
)
