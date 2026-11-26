/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val ANDROID_PACKAGE_HEADER_WINDOW = 4

internal object CertificateSha1Fingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "[B",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    filters = listOf(string("SHA1")),
)

internal object FirebaseInitProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/provider/FirebaseInitProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = listOf(),
)

internal object AppCheckTokenFingerprint : Fingerprint(
    definingClass = TOKEN_SEED_CLASS,
    name = "token",
)

internal object AndroidPackageHeaderFingerprint : Fingerprint(
    filters = listOf(
        string("X-Android-Package"),
        methodCall(
            definingClass = "Ljava/net/URLConnection;",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "V",
            location = MatchAfterWithin(ANDROID_PACKAGE_HEADER_WINDOW),
        ),
    ),
)
