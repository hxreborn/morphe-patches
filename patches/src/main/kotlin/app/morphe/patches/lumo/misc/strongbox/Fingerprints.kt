/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.strongbox

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object GenerateMasterKeyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljavax/crypto/SecretKey;",
    parameters = listOf("Ljavax/crypto/KeyGenerator;", "L", "Z"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/security/keystore/KeyGenParameterSpec\$Builder;",
            name = "setIsStrongBoxBacked",
        ),
    ),
)
