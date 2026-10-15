/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.viewmode

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ViewModeEnumFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("BRIEF", "FULL", "GRID", "THUMB"),
)

internal object CurrentViewModeFingerprint : Fingerprint(
    classFingerprint = ViewModeEnumFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
    ),
    custom = { method, classDef -> method.returnType == classDef.type },
)

internal object LibraryOptionsMenuFingerprint : Fingerprint(
    definingClass = "Lorg/readera/MainActivity;",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Landroid/view/Menu;"),
    filters = listOf(
        opcode(Opcode.CONST),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.XOR_INT_LIT8, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/MenuItem;", name = "setVisible", location = MatchAfterImmediately()),
    ),
)
