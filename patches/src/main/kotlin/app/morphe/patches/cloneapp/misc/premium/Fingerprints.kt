/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val USER_STATE_STRINGS = listOf("LAST_USER_INIT_TIME", "SUBS_TP")

internal object UserStateParseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lorg/json/JSONObject;"),
    strings = USER_STATE_STRINGS,
    filters = listOf(
        fieldAccess(definingClass = "this", type = "Z", opcode = Opcode.IPUT_BOOLEAN),
        string("IV", location = MatchAfterImmediately()),
        fieldAccess(definingClass = "this", type = "J", opcode = Opcode.IPUT_WIDE),
        string("ET", location = MatchAfterImmediately()),
    ),
)

internal object FCodeOfferFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lorg/json/JSONObject;"),
    strings = USER_STATE_STRINGS,
    filters = listOf(
        string("isi"),
        fieldAccess(
            definingClass = "this",
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN,
            location = MatchAfterWithin(5),
        ),
    ),
)

internal object UserStateRestoreFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = USER_STATE_STRINGS,
    filters = listOf(
        string("IV"),
        methodCall(name = "getBoolean", returnType = "Z", location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = "this",
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN,
            location = MatchAfterImmediately(),
        ),
        methodCall(
            parameters = listOf("Z"),
            returnType = "V",
            opcode = Opcode.INVOKE_INTERFACE,
            location = MatchAfterWithin(8),
        ),
        string("ET"),
        methodCall(name = "getLong", returnType = "J", location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = "this",
            type = "J",
            opcode = Opcode.IPUT_WIDE,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun engineVipStateSetterFingerprint(setter: MethodReference) = Fingerprint(
    name = setter.name,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.SPUT_BOOLEAN),
        fieldAccess(type = "Z", opcode = Opcode.SPUT_BOOLEAN, location = MatchAfterImmediately()),
    ),
)

internal fun staticInitializerFingerprint(definingClass: String) = Fingerprint(
    definingClass = definingClass,
    name = "<clinit>",
)

internal object DiscountOfferPopupFingerprint : Fingerprint(
    filters = listOf(
        string("default-50off"),
        methodCall(definingClass = "Ljava/lang/String;", name = "equals", location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
