/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.signature

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.fix.spoofsignature.isCertMaybeInauthentic
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.NoCertificateException
import app.morphe.util.getEndEntityCertificate
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

internal const val SPOOF_SIGNATURE_CLASS = "Lapp/hxreborn/extension/shared/SpoofSignature;"

private const val ANDROID_APPLICATION_CLASS = "Landroid/app/Application;"
private const val PACKAGE_NAME_PLACEHOLDER = "<package-name>"
private const val CERTIFICATE_PLACEHOLDER = "<certificate>"

internal fun BytecodePatchContext.spoofSignature(
    applicationClass: String,
    hostClass: String = SPOOF_SIGNATURE_CLASS,
) {
    val certificate = packageMetadata.stockSigningCertificate()

    mutableClassDefBy(SPOOF_SIGNATURE_CLASS)
        .methods.single { it.name == "<clinit>" }
        .apply {
            replaceString(PACKAGE_NAME_PLACEHOLDER, packageMetadata.packageName)
            replaceString(
                CERTIFICATE_PLACEHOLDER,
                Base64.getEncoder().encodeToString(certificate.encoded),
            )
        }

    applicationRoot(applicationClass).setSuperClass(hostClass)
}

internal fun PackageMetadata.stockSigningCertificate(): X509Certificate {
    val certificate = try {
        getEndEntityCertificate(signingCertificates)
    } catch (ex: NoCertificateException) {
        throw PatchException("The app being patched is not signed", ex)
    }

    if (isCertMaybeInauthentic(certificate)) {
        throw PatchException(
            "The app being patched was re-signed and no longer carries the developer certificate. " +
                "Patch a stock APK instead.",
        )
    }

    val declaredSha256 = declaredSignatures(packageName)
    if (declaredSha256.isNotEmpty()) {
        val certificateSha256 = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            .joinToString("") { "%02x".format(it) }
        if (certificateSha256 !in declaredSha256) {
            throw PatchException(
                "The app being patched is not signed with the $packageName developer certificate " +
                    "($certificateSha256). Patch a stock APK instead.",
            )
        }
    }

    return certificate
}

private fun declaredSignatures(packageName: String): Set<String> =
    AppCompatibilities::class.java.methods
        .filter { it.returnType == Compatibility::class.java && it.parameterTypes.isEmpty() }
        .map { it.invoke(AppCompatibilities) as Compatibility }
        .filter { it.packageName == packageName }
        .flatMap { it.signatures.orEmpty() }
        .toSet()

internal fun MutableMethod.replaceString(placeholder: String, value: String) {
    val index = indexOfFirstStringInstructionOrThrow(placeholder)
    val register = getInstruction<OneRegisterInstruction>(index).registerA

    replaceInstruction(index, "const-string v$register, \"$value\"")
}

private fun BytecodePatchContext.applicationRoot(applicationClass: String): MutableClass {
    var classDef = mutableClassDefBy(applicationClass)
    val walked = mutableSetOf(classDef.type)

    while (classDef.superclass != ANDROID_APPLICATION_CLASS) {
        val superclass = classDef.superclass
            ?: throw PatchException("Application hierarchy ended before android.app.Application")
        if (!walked.add(superclass)) {
            throw PatchException("Application hierarchy loops at $superclass")
        }
        classDef = mutableClassDefBy(superclass)
    }

    if (classDef.type == SPOOF_SIGNATURE_CLASS) {
        throw PatchException("Application hierarchy already extends $SPOOF_SIGNATURE_CLASS")
    }

    return classDef
}
