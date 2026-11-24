/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.fix.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MsaConfigurationFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lcom/microsoft/authentication/MsaConfiguration;",
            name = "<init>",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
        ),
    ),
)

internal object ConfigureMsalAuthenticatorFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/authentication/OneAuth;",
    name = "configureMsalAuthenticator",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "Lcom/microsoft/authentication/Error;",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/microsoft/authentication/internal/BrokerFactory;",
            name = "get",
            parameters = listOf("Landroid/content/Context;"),
        ),
        methodCall(
            definingClass = "Lcom/microsoft/identity/internal/PlatformComponents;",
            name = "setBroker",
        ),
        opcode(Opcode.GOTO, location = MatchAfterImmediately()),
    ),
)
