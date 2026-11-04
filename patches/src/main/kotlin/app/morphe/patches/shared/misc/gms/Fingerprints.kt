/*
 * Copyright (C) 2026 Morphe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/31a66d95932f6ccf4e39a5804c750af72a606d15
 * Commit 31a66d95932f6ccf4e39a5804c750af72a606d15 (2026-09-11),
 * patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/Fingerprints.kt
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object GetCredentialSuccessToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(string("GetCredentialSuccess(credential=")),
)

internal object GetCredentialFailureToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(string("GetCredentialFailure(type=")),
)

internal const val KOTLIN_RESULT_CLASS = "Lkotlin/Result;"

internal object ObfuscatedResultToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    filters = listOf(string("Success(")),
    custom = { _, classDef -> classDef.type != KOTLIN_RESULT_CLASS },
)

internal object GooglePlayServicesAvailabilityFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("This should never happen.", "MetadataValueReader"),
)

internal object GooglePlayServicesUtilAvailabilityFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("GooglePlayServicesUtil", " requires Google Play services, but their signature is invalid."),
)

private const val NO_ACTIVITY_FAILURE_WINDOW = 2

internal object GetCredentialHandlerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/String;", name = "isEmpty"),
        opcode(Opcode.IF_NEZ),
        string("No activity available", location = MatchAfterWithin(NO_ACTIVITY_FAILURE_WINDOW)),
        string("CredentialManager requires a serverClientId."),
        opcode(Opcode.INVOKE_STATIC, location = MatchAfterWithin(1)),
    ),
)

private const val PRE_U_MIN_GMS_APK_VERSION = 252400000

internal object PlayServicesGetCredentialRoutingFingerprint : Fingerprint(
    name = "onGetCredential",
    returnType = "V",
    filters = listOf(
        literal(PRE_U_MIN_GMS_APK_VERSION),
        methodCall(name = "isAvailableOnDevice", parameters = listOf("I"), returnType = "Z"),
    ),
)

internal object CredentialProviderFrameworkAvailabilityFingerprint : Fingerprint(
    name = "isAvailableOnDevice",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(fieldAccess(type = "Landroid/credentials/CredentialManager;")),
)
