/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedInstalledCertificateExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/shared/InstalledCertificate;"

val trustInstalledCertificatePatch = patch {
    dependsOn(sharedInstalledCertificateExtensionPatch)
    compatibleWith(AppCompatibilities.UPTCL)
    execute {
        SigningCertificateVerifierFingerprint.matchAll().forEach { match ->
            match.method.addInstructions(
                0,
                """
                    invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->appendSha256Hex(Landroid/content/Context;[Ljava/lang/String;)[Ljava/lang/String;
                    move-result-object p1
                """,
            )
        }
    }
}
