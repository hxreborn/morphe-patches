/*
 * SPDX-FileCopyrightText: 2026 hoo-dles
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Certificate selection ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/a2ba06a224b0cd918c9232e3d17738e07c208ff0
 * Commit a2ba06a224b0cd918c9232e3d17738e07c208ff0 (2026-09-04),
 * patches/src/main/kotlin/hoodles/morphe/util/ResourceUtils.kt
 */
package app.morphe.patches.shared.misc.signature

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.apk.ApkSignatureScheme
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

internal const val SPOOF_SIGNATURE_CLASS = "Lapp/hxreborn/extension/shared/SpoofSignature;"

private const val ANDROID_APPLICATION_CLASS = "Landroid/app/Application;"
private const val PACKAGE_NAME_PLACEHOLDER = "<package-name>"
private const val CERTIFICATE_PLACEHOLDER = "<certificate>"

private val INAUTHENTIC_CERTIFICATE_SHA1 = setOf(
    "e94e3afa40a54ecee4eef83f580393507fcd205a", // AntiSplit M
    "61ed377e85d386a8dfee6b864bd85b0bfaa5af81", // public Android debug certificate
)

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

private fun PackageMetadata.stockSigningCertificate(): X509Certificate {
    val certificates = signingCertificates.filterValues { it.isNotEmpty() }
    val scheme = certificates.keys.minByOrNull(::schemeOrder)
        ?: throw PatchException("The app being patched is not signed")

    val certificate = certificates.getValue(scheme).let { schemeCertificates ->
        if (schemeCertificates.size == 1) return@let schemeCertificates.single()

        val issuers = schemeCertificates.map { it.issuerX500Principal }.toSet()
        schemeCertificates.firstOrNull { it.subjectX500Principal !in issuers }
            ?: schemeCertificates.first()
    }

    if (certificate.isInauthentic()) {
        throw PatchException(
            "The app being patched was re-signed and no longer carries the developer certificate. " +
                "Patch a stock APK instead.",
        )
    }

    return certificate
}

private fun schemeOrder(scheme: ApkSignatureScheme) = when (scheme) {
    ApkSignatureScheme.V31 -> 0
    ApkSignatureScheme.V3 -> 1
    ApkSignatureScheme.V2 -> 2
    else -> Int.MAX_VALUE
}

private fun X509Certificate.isInauthentic(): Boolean {
    if (subjectX500Principal.name.contains("morphe", ignoreCase = true)) return true

    val sha1 = MessageDigest.getInstance("SHA-1").digest(encoded)
        .joinToString("") { "%02x".format(it) }

    return sha1 in INAUTHENTIC_CERTIFICATE_SHA1
}

private fun MutableMethod.replaceString(placeholder: String, value: String) {
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
