/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.messages

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object SendTextMessageFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(string("sendMessage: new JSONObject")),
)

internal object SendIntervalCheckFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(string("interrupted, ")),
)

internal object ChatInputLimitFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lcom/imo/android/common/widgets/BitmojiEditText;",
            name = "setTryDispatchCrash",
        ),
        methodCall(
            parameters = listOf("Landroid/widget/EditText;"),
            returnType = "V",
            opcode = Opcode.INVOKE_STATIC,
        ),
        string("ChatInputComponentV2"),
    ),
)
