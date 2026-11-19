/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ringtonemaker.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumUserFingerprint : Fingerprint(
    definingClass = "Lapp/better/ringtone/MainApplication;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Lcom/betterapp/googlebilling/SubsState;"),
            returnType = "Z",
        ),
    ),
)

internal object StartPurchaseScreenFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("vip_entry_click", "purchase_origin"),
)
