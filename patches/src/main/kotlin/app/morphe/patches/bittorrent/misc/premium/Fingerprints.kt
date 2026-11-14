/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object RestoreProStatusFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("PRO_PAID", "PRO_UNPAID"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getString"),
        fieldAccess(definingClass = "this", opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun proStatusReadFingerprint(status: FieldReference) = Fingerprint(
    filters = listOf(fieldAccess(reference = status, opcode = Opcode.SGET_OBJECT)),
)
