/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumUserFingerprint : Fingerprint(
    definingClass = "Lapp/better/ringtone/MainApplication;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        opcode(Opcode.SGET_BOOLEAN, InstructionLocation.MatchFirst()),
        opcode(Opcode.INVOKE_STATIC, InstructionLocation.MatchAfterWithin(2)),
    ),
)

internal object StartPurchaseScreenFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/content/Context;"),
    strings = listOf("vip_entry_click"),
)
