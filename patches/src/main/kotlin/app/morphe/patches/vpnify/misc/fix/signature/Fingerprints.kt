/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.fix.signature

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object CertificateDigestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("X509", "SHA1"),
)
