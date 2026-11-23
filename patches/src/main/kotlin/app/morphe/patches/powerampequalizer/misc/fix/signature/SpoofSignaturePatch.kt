/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.powerampequalizer.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.signature.stockSigningCertificate

private const val APPLICATION_CLASS = "Lcom/maxmpz/equalizer/PeqApplication;"

private val spoofNativeSigningCertificatePatch = patch {
    execute {
        val library = resourceFile(NativeSigningCertificate.LIBRARY, true)
        if (!library.exists()) throw PatchException("${NativeSigningCertificate.LIBRARY} is missing")

        val stockCertificate = packageMetadata.stockSigningCertificate().encoded
        library.writeBytes(NativeSigningCertificate.withStockCertificate(library.readBytes(), stockCertificate))
    }
}

internal val spoofSignaturePatch = patch {
    dependsOn(sharedSignatureExtensionPatch, spoofNativeSigningCertificatePatch)
    compatibleWith(AppCompatibilities.POWERAMP_EQUALIZER)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
