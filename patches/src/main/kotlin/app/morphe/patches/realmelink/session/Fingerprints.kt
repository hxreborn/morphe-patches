/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ReLoginPromptFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Landroid/content/Context;", "Lkotlin/coroutines/Continuation;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/realme/iot/account/R\$string;",
            name = "account_relogin_for_security",
            type = "I",
            opcode = Opcode.SGET,
        ),
    ),
)
