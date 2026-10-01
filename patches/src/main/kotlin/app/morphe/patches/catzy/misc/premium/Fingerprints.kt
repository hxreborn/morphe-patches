/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val VIP_INFO_CLASS = "Lcom/nieruo/healthapp/entity/VipInfo;"

internal fun vipTimeCheckFingerprint(getter: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = VIP_INFO_CLASS, name = getter, returnType = "J"),
        opcode(Opcode.CMP_LONG),
        opcode(Opcode.IF_LEZ),
    ),
)
