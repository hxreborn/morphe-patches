/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumStateFingerprint : Fingerprint(
    name = "invokeSuspend",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/System;", name = "currentTimeMillis"),
        opcode(Opcode.CMP_LONG),
        methodCall(definingClass = "Landroid/text/TextUtils;", name = "isEmpty"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
