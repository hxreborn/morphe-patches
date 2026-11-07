/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TYPING_MEMBERSHIP_READ_WINDOW = 3

internal object AppIconLockedFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/feature/viberplus/presentation/settings/model/ViberPlusAppIconUi;",
    name = "isLocked",
    parameters = emptyList(),
    returnType = "Z",
)

internal object TypingPrivacyControllerFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("viber_plus_hide_is_typing_ftue_dismissed"),
)

internal object TypingPrivacyExperimentFingerprint : Fingerprint(
    classFingerprint = TypingPrivacyControllerFingerprint,
    parameters = emptyList(),
    returnType = "Z",
    filters = listOf(methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue")),
)

internal fun typingPrivacyStateFingerprint(controllerType: String, experimentName: String) = Fingerprint(
    name = "invokeSuspend",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = controllerType, name = experimentName),
        methodCall(definingClass = controllerType, parameters = emptyList(), returnType = "Z"),
        methodCall(parameters = emptyList(), returnType = "Z", location = MatchAfterWithin(TYPING_MEMBERSHIP_READ_WINDOW)),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal fun typingSendAllowedFingerprint(
    controllerType: String,
    experimentName: String,
    membership: MethodReference,
) = Fingerprint(
    parameters = listOf("I"),
    returnType = "Z",
    filters = listOf(
        methodCall(membership),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(definingClass = controllerType, name = experimentName),
        methodCall(membership),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
