/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.opal.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val USERS_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS `users` ("
internal const val ONBOARDING_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS `onboarding` ("

internal object FirebaseAuthStoreFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/auth/FirebaseAuth;",
    name = "<init>",
    filters = listOf(
        string("com.google.firebase.auth.api.Store."),
        methodCall(
            definingClass = "Landroid/content/Context;",
            name = "getSharedPreferences",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object CreateAllTablesFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        string(USERS_TABLE_STATEMENT, StringComparisonType.STARTS_WITH),
        string(ONBOARDING_TABLE_STATEMENT, StringComparisonType.STARTS_WITH),
    ),
)

internal object OpalUserToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("OpalUser(id="),
)

internal object IsProfileIncompleteFingerprint : Fingerprint(
    classFingerprint = OpalUserToStringFingerprint,
    returnType = "Z",
    parameters = emptyList(),
)
