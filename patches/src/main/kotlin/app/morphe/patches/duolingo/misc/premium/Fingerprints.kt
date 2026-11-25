/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method

private const val USER_CLASS = "Lcom/duolingo/data/user/User;"
internal const val SUBSCRIBER_LEVEL_CLASS = "Lcom/duolingo/data/user/SubscriberLevel;"
private const val HAS_PLUS_READ_WINDOW = 6
private const val IS_PAID_WRITE_WINDOW = 12
private const val HAS_MAX_WRITE_WINDOW = 24
private const val ENERGY_CONFIG_CLASS = "Lcom/duolingo/core/energy/models/EnergyConfig;"

private val startsWithIntParameter: (Method, ClassDef) -> Boolean =
    { method, _ -> method.parameterTypes.firstOrNull() == "I" }

internal object UserIsPaidFingerprint : Fingerprint(
    definingClass = USER_CLASS,
    name = "<init>",
    custom = startsWithIntParameter,
    filters = listOf(
        fieldAccess(definingClass = USER_CLASS, type = SUBSCRIBER_LEVEL_CLASS, opcode = Opcode.IPUT_OBJECT),
        methodCall(definingClass = SUBSCRIBER_LEVEL_CLASS, name = "hasSubscription"),
        fieldAccess(definingClass = USER_CLASS, opcode = Opcode.IGET_BOOLEAN, location = MatchAfterWithin(HAS_PLUS_READ_WINDOW)),
        fieldAccess(definingClass = USER_CLASS, opcode = Opcode.IPUT_BOOLEAN, location = MatchAfterWithin(IS_PAID_WRITE_WINDOW)),
    ),
)

internal object UserHasMaxFingerprint : Fingerprint(
    definingClass = USER_CLASS,
    name = "<init>",
    custom = startsWithIntParameter,
    filters = listOf(
        fieldAccess(definingClass = SUBSCRIBER_LEVEL_CLASS, name = "GOLD", opcode = Opcode.SGET_OBJECT),
        fieldAccess(definingClass = USER_CLASS, opcode = Opcode.IPUT_BOOLEAN, location = MatchAfterWithin(HAS_MAX_WRITE_WINDOW)),
    ),
)

internal object LoggedInToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("LoggedIn(user="),
)

internal object LoggedInConstructorFingerprint : Fingerprint(
    classFingerprint = LoggedInToStringFingerprint,
    name = "<init>",
    parameters = listOf(USER_CLASS),
)

internal object EnergyConfigConstructorsFingerprint : Fingerprint(
    definingClass = ENERGY_CONFIG_CLASS,
    name = "<init>",
    filters = listOf(
        fieldAccess(definingClass = ENERGY_CONFIG_CLASS, type = "I", opcode = Opcode.IPUT),
        fieldAccess(
            definingClass = ENERGY_CONFIG_CLASS,
            type = "I",
            opcode = Opcode.IPUT,
            location = MatchAfterImmediately(),
        ),
    ),
)
