/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.selection

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun textSearchHintFingerprint(hint: Long) = Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED),
    returnType = "I",
    parameters = listOf(),
    filters = listOf(literal(hint)),
)

internal fun textSearchOpenFingerprint(dialog: String) = Fingerprint(
    definingClass = dialog,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(newInstance(dialog)),
)

internal fun textSearchSubmitFingerprint(dialogBase: String) = Fingerprint(
    definingClass = dialogBase,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal fun textSearchViewCreatedFingerprint(dialog: String) = Fingerprint(
    definingClass = dialog,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
)

internal fun textSearchShowFingerprint(dialog: String) = Fingerprint(
    definingClass = dialog,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(parameters = listOf("Landroid/app/Dialog;"), returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_SUPER, parameters = listOf(), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal fun quoteShareFingerprint(subject: Long) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf(), location = MatchFirst()),
        literal(subject),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "Ljava/lang/String;"),
    ),
)
