/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
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

internal val sessionAuthCall = methodCall(
    opcode = Opcode.INVOKE_STATIC,
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "Lio/reactivex/Observable;",
)

internal val resultSuccessCall = methodCall(
    opcode = Opcode.INVOKE_VIRTUAL,
    parameters = emptyList(),
    returnType = "Z",
    location = MatchAfterWithin(2),
)

internal val accountServiceCall = methodCall(
    opcode = Opcode.INVOKE_STATIC,
    parameters = emptyList(),
    location = MatchAfterWithin(20),
)

internal val linkTokenRead = fieldAccess(
    opcode = Opcode.IGET_OBJECT,
    type = "Ljava/lang/Object;",
    location = MatchAfterWithin(6),
)

internal val storeTokensCall = methodCall(
    opcode = Opcode.INVOKE_VIRTUAL,
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "V",
    location = MatchAfterWithin(4),
)

internal object LinkServerAuthFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Lkotlin/coroutines/Continuation;"),
    filters = listOf(
        sessionAuthCall,
        methodCall(name = "getMessage", returnType = "Ljava/lang/String;"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterWithin(2),
        ),
        resultSuccessCall,
        string("authToServerLink success", location = MatchAfterWithin(5)),
        accountServiceCall,
        linkTokenRead,
        storeTokensCall,
    ),
)

internal val heytapTokenRead = fieldAccess(
    opcode = Opcode.IGET_OBJECT,
    type = "Ljava/lang/String;",
    location = MatchAfterWithin(2),
)

internal object AccountTokensToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("AccountInfo(token="),
        string(", oppoToken=", location = MatchAfterWithin(4)),
        heytapTokenRead,
    ),
)
