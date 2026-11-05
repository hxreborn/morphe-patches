/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.security.MessageDigest

private const val APPLICATION_CLASS = "Lcom/xt/retouch/MainApplication;"

private const val APK_SIGNATURE_V3_PREFIX = "S3:"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.HYPIC)
    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)

        val stockPublicKeyMd5Hex = MessageDigest.getInstance("MD5")
            .digest(packageMetadata.stockSigningCertificate().publicKey.encoded)
            .joinToString("") { "%02X".format(it) }

        ApkFileSignatureReportFingerprint.matchSingle().let { match ->
            val (_, nativeSignatureResult) = match.instructionMatches
            val signatureRegister = match.method.getInstruction<OneRegisterInstruction>(nativeSignatureResult.index).registerA
            match.method.addInstruction(
                nativeSignatureResult.index + 1,
                "const-string v$signatureRegister, \"$APK_SIGNATURE_V3_PREFIX$stockPublicKeyMd5Hex\"",
            )
        }

        SecuritySdkInitFingerprint.matchSingle().let { match ->
            val (configBuilderNewInstance) = match.instructionMatches
            match.method.addInstruction(configBuilderNewInstance.index, "return-void")
        }
    }
}
