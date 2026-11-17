/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = emptyList(),
    strings = listOf("devcheck.monthly", "devcheck.annual"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/HashMap;", name = "get"),
    ),
)

internal object IsPremiumFingerprint : Fingerprint(
    classFingerprint = PremiumStateFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "equals"),
    ),
)

internal fun liveDataConstructorFingerprint(liveDataType: String) = Fingerprint(
    definingClass = liveDataType,
    name = "<init>",
    returnType = "V",
    parameters = listOf("B"),
)

internal fun liveDataModeFingerprint(mediatorType: String, liveDataType: String) = Fingerprint(
    definingClass = mediatorType,
    name = "<init>",
    filters = listOf(
        opcode(Opcode.CONST_4),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            definingClass = liveDataType,
            name = "<init>",
            parameters = listOf("B"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object LiveDataSetValueFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("setValue"),
    custom = { _, classDef -> AccessFlags.ABSTRACT.isSet(classDef.accessFlags) },
)

internal object SigningCertificateDigestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("Lflar2/devcheck/MainActivity;"),
    strings = listOf("SHA"),
)
