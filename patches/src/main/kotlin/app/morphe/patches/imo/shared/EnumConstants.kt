/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.string
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private fun enumConstantFingerprint(constantName: String) = Fingerprint(
    filters = listOf(
        string(constantName),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

context(_: BytecodePatchContext)
internal fun enumConstantField(constantName: String) = enumConstantFingerprint(constantName)
    .matchSingle().instructionMatches.last().instruction.getReference<FieldReference>()!!
