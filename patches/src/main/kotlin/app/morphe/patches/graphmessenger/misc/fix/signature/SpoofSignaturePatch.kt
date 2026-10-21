/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.security.MessageDigest

private const val APPLICATION_CLASS = "Lorg/telegram/messenger/ApplicationLoaderImpl;"

private val spoofNativeCertificateDigestPatch = rawResourcePatch {
    execute {
        val library = get(NativeCertificateChecks.LIBRARY, true)
        if (!library.exists()) throw PatchException("${NativeCertificateChecks.LIBRARY} is missing")

        val stockCertificateDigest = MessageDigest.getInstance("SHA-256")
            .digest(packageMetadata.stockSigningCertificate().encoded)
        val bytes = library.readBytes()
        NativeCertificateChecks.spoofCertificateDigest(bytes, stockCertificateDigest)
        library.writeBytes(bytes)
    }
}

internal val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    extendWith("extensions/extension.mpe")

    dependsOn(spoofNativeCertificateDigestPatch)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
