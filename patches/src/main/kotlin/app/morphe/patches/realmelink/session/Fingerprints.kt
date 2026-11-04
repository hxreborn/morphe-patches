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

private const val CALL_RESULT_WINDOW = 2

private const val SUCCESS_BRANCH_WINDOW = 5

private const val SUCCESS_LOG_WINDOW = 20

private const val ACCOUNT_HELPER_CAST_WINDOW = 6

private const val LINK_TOKEN_NULL_CHECK_WINDOW = 4

private const val ACCESS_TOKEN_APPEND_WINDOW = 4

private const val LABEL_APPEND_WINDOW = 2

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
    location = MatchAfterWithin(CALL_RESULT_WINDOW),
)

internal val accountServiceCall = methodCall(
    opcode = Opcode.INVOKE_STATIC,
    parameters = emptyList(),
    location = MatchAfterWithin(SUCCESS_LOG_WINDOW),
)

internal val linkTokenRead = fieldAccess(
    opcode = Opcode.IGET_OBJECT,
    type = "Ljava/lang/Object;",
    location = MatchAfterWithin(ACCOUNT_HELPER_CAST_WINDOW),
)

internal val storeTokensCall = methodCall(
    opcode = Opcode.INVOKE_VIRTUAL,
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "V",
    location = MatchAfterWithin(LINK_TOKEN_NULL_CHECK_WINDOW),
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
            location = MatchAfterWithin(CALL_RESULT_WINDOW),
        ),
        resultSuccessCall,
        string("authToServerLink success", location = MatchAfterWithin(SUCCESS_BRANCH_WINDOW)),
        accountServiceCall,
        linkTokenRead,
        storeTokensCall,
    ),
)

internal object SdkHeytapTokenFingerprint : Fingerprint(
    definingClass = "Lcom/heytap/msp/sdk/AccountSdk;",
    name = "getToken",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal val heytapTokenRead = fieldAccess(
    opcode = Opcode.IGET_OBJECT,
    type = "Ljava/lang/String;",
    location = MatchAfterWithin(LABEL_APPEND_WINDOW),
)

internal object AccountTokensToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("AccountInfo(token="),
        string(", oppoToken=", location = MatchAfterWithin(ACCESS_TOKEN_APPEND_WINDOW)),
        heytapTokenRead,
    ),
)
