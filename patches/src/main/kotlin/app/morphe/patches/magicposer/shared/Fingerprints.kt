/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicposer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val MAIN_ACTIVITY_CLASS = "Lcom/magicposernew/MainActivity;"
private const val INT_ARRAY_TYPE = "[I"
private const val TOKEN_SLOT_GAP = 1
private const val INTEGER_TYPE = "Ljava/lang/Integer;"
private const val REFLECT_METHOD_TYPE = "Ljava/lang/reflect/Method;"
private const val TOAST_TYPE = "Landroid/widget/Toast;"
private const val TOAST_TEXT_WINDOW = 3

internal object MainActivityCreateFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

internal object MainActivityAttachBaseContextFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "attachBaseContext",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal fun tokenComparisonFingerprint() = Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        opcode(Opcode.AGET_OBJECT),
        checkCast(INT_ARRAY_TYPE, location = MatchAfterImmediately()),
        opcode(Opcode.AGET, location = MatchAfterWithin(TOKEN_SLOT_GAP)),
        opcode(Opcode.AGET_OBJECT, location = MatchAfterWithin(TOKEN_SLOT_GAP)),
        checkCast(INT_ARRAY_TYPE, location = MatchAfterImmediately()),
        opcode(Opcode.AGET, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, location = MatchAfterImmediately()),
    ),
)

internal fun toastCrashFingerprint(name: String, parameters: List<String>) = Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = name,
    returnType = "V",
    parameters = parameters,
    filters = listOf(
        checkCast(INTEGER_TYPE),
        methodCall(INTEGER_TYPE, "intValue", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(TOAST_TYPE, "makeText", location = MatchAfterWithin(TOAST_TEXT_WINDOW)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(TOAST_TYPE, "show", location = MatchAfterImmediately()),
    ),
)

internal object ProtectionThreadStartFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(REFLECT_METHOD_TYPE, "invoke"),
        methodCall(name = "onResume", location = MatchAfterImmediately()),
    ),
)

internal object ReviewFlowLaunchFingerprint : Fingerprint(
    parameters = listOf("Landroid/app/Activity;", "L"),
    strings = listOf("confirmation_intent", "window_flags", "result_receiver"),
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
    ),
)
