/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal fun constructorWritingFingerprint(field: FieldReference) = Fingerprint(
    definingClass = field.definingClass,
    name = "<init>",
    filters = listOf(fieldAccess(reference = field, opcodes = listOf(Opcode.IPUT_BOOLEAN, Opcode.IPUT_OBJECT))),
)
