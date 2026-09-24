/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.echogram.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object EntitlementSyncFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Ljava/util/Map;"),
    strings = listOf("activeStore"),
    filters = listOf(
        string("isPremium"),
        fieldAccess(type = "Z", opcode = Opcode.SPUT_BOOLEAN),
    ),
)

internal fun premiumStateInitializerFingerprint(premiumStateClass: String) = Fingerprint(
    definingClass = premiumStateClass,
    name = "<clinit>",
    filters = listOf(
        methodCall(parameters = listOf("Ljava/lang/Object;"), opcode = Opcode.INVOKE_STATIC),
        fieldAccess(definingClass = "this", opcode = Opcode.SPUT_OBJECT, location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
    ),
)
