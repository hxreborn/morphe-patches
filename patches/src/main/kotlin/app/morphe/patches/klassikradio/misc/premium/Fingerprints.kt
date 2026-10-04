/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.klassikradio.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SubscriptionTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("NONE", "LIVE"),
    filters = listOf(
        string("PREMIUM"),
        fieldAccess(definingClass = "this", opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun contractConstructorFingerprint(subscriptionType: String) = Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        fieldAccess(type = subscriptionType, opcode = Opcode.IPUT_OBJECT),
    ),
    custom = { method, _ -> method.parameterTypes.firstOrNull() == subscriptionType },
)

internal object SectionTypeDecoderFingerprint : Fingerprint(
    name = "deserialize",
    returnType = "Ljava/lang/Object;",
    strings = listOf("itemType", "category"),
    filters = listOf(
        string("banner"),
        opcode(Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)

internal object SkipToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("Skip(availableSkips=", ", skipValid="),
)
