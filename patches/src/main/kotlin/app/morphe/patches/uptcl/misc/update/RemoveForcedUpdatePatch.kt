/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.uptcl.misc.fix.signature.trustInstalledCertificatePatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/uptcl/AppVersionCheck;"

@Suppress("unused")
val removeForcedUpdatePatch = bytecodePatch(
    name = "Remove forced update",
    description = "Removes the update prompts that block the app at launch.",
) {
    compatibleWith(AppCompatibilities.UPTCL)
    dependsOn(trustInstalledCertificatePatch)
    extendWith("extensions/extension.mpe")

    execute {
        AppVersionResultFingerprint.matchSingle().method.apply {
            findInstructionIndicesReversedOrThrow(
                methodCall(definingClass = "Ljava/lang/String;", name = "hashCode"),
            ).forEach { index ->
                val codeRegister = getInstruction<FiveRegisterInstruction>(index).registerC
                addInstructions(
                    index,
                    """
                        invoke-static/range { v$codeRegister .. v$codeRegister }, $EXTENSION_CLASS_DESCRIPTOR->replaceUpdateCode(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$codeRegister
                    """,
                )
            }
        }
    }
}
