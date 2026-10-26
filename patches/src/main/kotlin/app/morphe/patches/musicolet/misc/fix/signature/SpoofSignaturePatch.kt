/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.util.Base64

private const val APPLICATION_CLASS = "Lin/krosbits/musicolet/MyApplication;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.MUSICOLET)
    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)

        val certificate = Base64.getEncoder().encodeToString(packageMetadata.stockSigningCertificate().encoded)
        listOf(CertificateDigestFingerprint, CertificateChecksumFingerprint).forEach { fingerprint ->
            fingerprint.matchSingle().method.apply {
                val decodeFlags = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                addInstructions(
                    0,
                    """
                        const-string p0, "$certificate"
                        const/4 v$decodeFlags, 0x0
                        invoke-static { p0, v$decodeFlags }, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
                        move-result-object p0
                    """,
                )
            }
        }
    }
}
