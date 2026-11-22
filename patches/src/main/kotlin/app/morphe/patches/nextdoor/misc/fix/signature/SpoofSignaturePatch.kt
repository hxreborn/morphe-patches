/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.security.MessageDigest
import java.util.Base64

internal val spoofSignaturePatch = patch {
    compatibleWith(AppCompatibilities.NEXTDOOR)

    execute {
        val stockCertificateSha1Base64 = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(packageMetadata.stockSigningCertificate().encoded),
        )

        PackageCertificateHashFingerprint.method.apply {
            val registers = getFreeRegisterProvider(0, 3)
            val packageMatches = registers.getFreeRegister4Bit()
            val certificate = registers.getFreeRegister4Bit()
            val base64Flags = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    const-string v$packageMatches, "${packageMetadata.packageName}"
                    invoke-virtual { v$packageMatches, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v$packageMatches
                    if-eqz v$packageMatches, :original
                    const-string v$certificate, "$stockCertificateSha1Base64"
                    const/4 v$base64Flags, 0x0
                    invoke-static { v$certificate, v$base64Flags }, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
                    move-result-object v$certificate
                    return-object v$certificate
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }
    }
}
