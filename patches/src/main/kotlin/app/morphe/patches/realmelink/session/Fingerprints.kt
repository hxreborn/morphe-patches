/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
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

internal object ReLoginToastFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("BUS_TYPE_TOKEN_INVALID invalidateToken = "),
        resourceLiteral(ResourceType.STRING, "account_relogin_for_security"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("I"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
    ),
)
