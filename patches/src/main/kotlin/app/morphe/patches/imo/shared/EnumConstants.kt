/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private fun enumConstantFingerprint(constantName: String) = Fingerprint(
    filters = listOf(
        string(constantName),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun enumConstantField(constantName: String): FieldReference {
    val (_, enumFieldStore) = enumConstantFingerprint(constantName).match().instructionMatches
    return enumFieldStore.fieldReference
}
