/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.checkCast
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object RecaptchaTokenRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lkotlin/jvm/functions/Function2;", "L"),
    strings = listOf("RECAPTCHA_ACTION_NULL"),
)

internal object IgnoredClearCredentialErrorFingerprint : Fingerprint(
    name = "onError",
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("Landroidx/credentials/exceptions/ClearCredentialException;"),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately()),
    ),
)
