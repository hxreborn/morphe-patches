/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.security.MessageDigest
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lorg/telegram/messenger/ApplicationLoaderImpl;"

private val spoofNativeCertificateDigestPatch = patch {
    execute {
        val library = resourceFile(NativeCertificateChecks.LIBRARY, true)
        if (!library.exists()) throw PatchException("${NativeCertificateChecks.LIBRARY} is missing")

        val stockCertificateDigest = MessageDigest.getInstance("SHA-256")
            .digest(packageMetadata.stockSigningCertificate().encoded)
        val bytes = library.readBytes()
        NativeCertificateChecks.spoofCertificateDigest(bytes, stockCertificateDigest)
        library.writeBytes(bytes)
    }
}

internal val spoofSignaturePatch = patch {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)

    dependsOn(spoofNativeCertificateDigestPatch)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
