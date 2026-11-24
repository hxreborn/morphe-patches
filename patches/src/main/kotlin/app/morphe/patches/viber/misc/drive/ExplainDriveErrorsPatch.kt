/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.drive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.viberSettingsExtensionPatch
import app.morphe.patches.shared.misc.requireStandardInstall
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/viber/AppliedPatches;"

private const val GMS_CORE_REQUIRED_MESSAGE = "Google Drive backup and restore need the GmsCore support patch."

private val driveErrorDialogFingerprints = listOf(
    driveErrorDialogFingerprint("D437", "dialog_437_message"),
    driveErrorDialogFingerprint("D438", "dialog_438_message"),
)

@Suppress("unused")
val explainDriveErrorsPatch: Patch = patch(
    name = "Explain Google Drive errors",
    description = "Shows how to fix Google Drive backup and restore errors when GmsCore support is not applied.",
) {
    dependsOn(viberSettingsExtensionPatch)
    compatibleWith(AppCompatibilities.VIBER)
    availability(requireStandardInstall)
    dependsOn(spoofSignaturePatch)

    execute {
        markPatchApplied("explainDriveErrors", explainDriveErrorsPatch)
        driveErrorDialogFingerprints.forEach { fingerprint ->
            fingerprint.match().apply {
                val (_, messageSetter, _) = instructionMatches
                val messageSetterCall = messageSetter.instruction as FiveRegisterInstruction
                val builderClass = messageSetter.instruction.getReference<MethodReference>()!!.definingClass
                val messageTextSetter = dialogMessageTextSetterFingerprint(builderClass).method
                val builderRegister = messageSetterCall.registerC
                val messageRegister = messageSetterCall.registerD

                method.addInstructionsWithLabels(
                    messageSetter.index + 1,
                    """
                        invoke-static {}, $APPLIED_PATCHES_CLASS->gmsCoreSupport()Z
                        move-result v$messageRegister
                        if-nez v$messageRegister, :keep_stock_message
                        const-string v$messageRegister, "$GMS_CORE_REQUIRED_MESSAGE"
                        invoke-virtual {v$builderRegister, v$messageRegister}, $builderClass->${messageTextSetter.name}(Ljava/lang/CharSequence;)V
                    """,
                    ExternalLabel("keep_stock_message", method.getInstruction(messageSetter.index + 1)),
                )
            }
        }
    }
}
