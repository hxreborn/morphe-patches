/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castle.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.secshell.editSecShellPayload
import app.morphe.patches.shared.misc.secshell.secShellPayloadPatch
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import com.android.tools.smali.dexlib2.AccessFlags

private const val SIGNATURE_CLASS = "Landroid/content/pm/Signature;"
private const val DIGEST_ERROR = "error!"

@Suppress("unused")
val spoofSignaturePatch = patch(
    name = "Spoof signature",
    description = "Removes the modified app warning that stops video playback.",
) {
    compatibleWith(AppCompatibilities.CASTLE)

    dependsOn(secShellPayloadPatch)

    execute {
        val certificate = packageMetadata.stockSigningCertificate().encoded.joinToString("") { "%02x".format(it) }

        editSecShellPayload {
            val digests = methodsUsingString(DIGEST_ERROR, firstParameter = SIGNATURE_CLASS)
            if (digests.isEmpty()) throw PatchException("No signature digest method uses \"$DIGEST_ERROR\"")

            digests.forEach { digest ->
                if (!AccessFlags.STATIC.isSet(digest.accessFlags)) {
                    throw PatchException("${digest.definingClass}->${digest.name} is not static")
                }
                digest.addInstructions(
                    0,
                    """
                        new-instance v0, $SIGNATURE_CLASS
                        const-string v1, "$certificate"
                        invoke-direct { v0, v1 }, $SIGNATURE_CLASS-><init>(Ljava/lang/String;)V
                        move-object p0, v0
                    """,
                )
            }
        }
    }
}
