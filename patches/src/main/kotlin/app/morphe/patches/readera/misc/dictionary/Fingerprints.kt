/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.dictionary

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object DrawerMenuVisibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/view/Menu;", name = "setGroupVisible"),
        methodCall(definingClass = "Landroid/view/MenuItem;", name = "setVisible"),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/MenuItem;", name = "setVisible", location = MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterImmediately()),
    ),
)
