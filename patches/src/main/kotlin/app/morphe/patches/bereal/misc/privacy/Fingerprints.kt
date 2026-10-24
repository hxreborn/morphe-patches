/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterLast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val SHAKE_CLASS = "Lcom/shakebugs/shake/Shake;"

internal object ShakeStartCallFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            definingClass = SHAKE_CLASS,
            name = "start",
            parameters = listOf("Landroid/app/Application;", "Ljava/lang/String;"),
            returnType = "V",
        ),
    ),
    custom = { _, classDef -> classDef.type != SHAKE_CLASS },
)

internal object AmplitudeUploadFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("https://api.eu.amplitude.com/2/httpapi", "https://api2.amplitude.com/2/httpapi"),
)

internal fun amplitudeResponseFactoryFingerprint(responseType: String) = Fingerprint(
    isStatic = true,
    returnType = responseType,
    parameters = listOf("I", "Ljava/lang/String;"),
)

internal object TrackingConsentEnumFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("GRANTED", "NOT_GRANTED", "PENDING"),
)

internal fun trackingConsentSetterFingerprint(consentType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "V",
    parameters = listOf(consentType),
    filters = listOf(methodCall(opcode = Opcode.INVOKE_INTERFACE, definingClass = "Ljava/lang/Iterable;", name = "iterator")),
)

internal object DatadogUploadRequestFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("L", "L"),
    strings = listOf("DD-API-KEY", "User-Agent"),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterLast()),
        methodCall(opcode = Opcode.INVOKE_DIRECT, name = "<init>", location = MatchAfterImmediately()),
    ),
)

internal object AdjustInitFingerprint : Fingerprint(
    definingClass = "Lcom/adjust/sdk/Adjust;",
    name = "initSdk",
    returnType = "V",
    parameters = listOf("Lcom/adjust/sdk/AdjustConfig;"),
)
