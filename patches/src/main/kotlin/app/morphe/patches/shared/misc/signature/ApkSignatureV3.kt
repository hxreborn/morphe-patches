/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.signature

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.patch.PatchException
import java.io.ByteArrayInputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

private const val SIGNATURE_BLOCK_FIELD = "signatureBlock"
private const val V3_SIGNATURE_ID = 0xf05368c0.toInt()
private const val PROOF_OF_ROTATION_ATTRIBUTE_ID = 0x3ba06f8c
private const val PROOF_OF_ROTATION_VERSION = 1
private const val UINT32_BYTES = 4

internal class V3Signer(val certificate: X509Certificate, val lineage: List<X509Certificate>)

internal fun PackageMetadata.readV3Signer(): V3Signer? {
    val schemeBytes = v3SchemeBytes() ?: return null
    return try {
        parseV3Signer(schemeBytes)
    } catch (ex: BufferUnderflowException) {
        throw PatchException("The APK Signature Scheme v3 block is truncated", ex)
    } catch (ex: CertificateException) {
        throw PatchException("The APK Signature Scheme v3 block holds an unreadable certificate", ex)
    }
}

private fun PackageMetadata.v3SchemeBytes(): ByteArray? {
    val signatureBlock = try {
        PackageMetadata::class.java.getDeclaredField(SIGNATURE_BLOCK_FIELD)
            .apply { isAccessible = true }
            .get(this)
    } catch (ex: ReflectiveOperationException) {
        throw PatchException("Cannot read the APK signing block from the package metadata", ex)
    } ?: return null
    val v3Signature = (signatureBlock as Iterable<*>).firstOrNull { it?.callGetter("getIdValue") == V3_SIGNATURE_ID }
        ?: return null
    return v3Signature.callGetter("getSignatureScheme")?.callGetter("getBytes") as ByteArray?
}

private fun Any.callGetter(name: String): Any? = try {
    javaClass.getMethod(name).invoke(this)
} catch (ex: ReflectiveOperationException) {
    throw PatchException("Cannot call $name on ${javaClass.name} in the APK signing block", ex)
}

internal fun parseV3Signer(schemeBytes: ByteArray): V3Signer {
    val signers = ByteBuffer.wrap(schemeBytes).order(ByteOrder.LITTLE_ENDIAN).lengthPrefixed()
    val signedData = signers.lengthPrefixed().lengthPrefixed()
    signedData.lengthPrefixed()
    val certificates = signedData.lengthPrefixed()
    signedData.position(signedData.position() + 2 * UINT32_BYTES)
    val attributes = signedData.lengthPrefixed()

    val factory = CertificateFactory.getInstance("X.509")
    val certificate = factory.certificate(certificates.lengthPrefixed())
    var lineage = emptyList<X509Certificate>()
    while (attributes.hasRemaining()) {
        val attribute = attributes.lengthPrefixed()
        if (attribute.int == PROOF_OF_ROTATION_ATTRIBUTE_ID) {
            lineage = parseProofOfRotation(attribute, factory)
        }
    }
    return V3Signer(certificate, lineage)
}

private fun parseProofOfRotation(attribute: ByteBuffer, factory: CertificateFactory): List<X509Certificate> {
    val version = attribute.int
    if (version != PROOF_OF_ROTATION_VERSION) {
        throw PatchException("Unsupported APK signing lineage version $version")
    }
    return buildList {
        while (attribute.hasRemaining()) {
            val signedData = attribute.lengthPrefixed().lengthPrefixed()
            add(factory.certificate(signedData.lengthPrefixed()))
        }
    }
}

private fun CertificateFactory.certificate(der: ByteBuffer): X509Certificate {
    val bytes = ByteArray(der.remaining()).also(der::get)
    return generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
}

private fun ByteBuffer.lengthPrefixed(): ByteBuffer {
    val length = int
    if (length < 0 || length > remaining()) {
        throw PatchException("APK signing block entry of $length bytes overruns its ${remaining()}-byte parent")
    }
    val entry = slice().order(ByteOrder.LITTLE_ENDIAN)
    entry.limit(length)
    position(position() + length)
    return entry
}
