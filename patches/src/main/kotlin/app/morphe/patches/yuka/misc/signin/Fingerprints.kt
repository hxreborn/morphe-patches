/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

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
