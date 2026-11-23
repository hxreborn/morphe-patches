/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.templates

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val openEmailTemplatesPatch: Patch = patch(
    name = "Open email templates",
    description = "Opens Outlook template (.oft) attachments as new drafts without a Microsoft 365 subscription.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("openEmailTemplates", openEmailTemplatesPatch)
        val isSupportedOftFile = IsSupportedOftFileFingerprint.match()
        val (_, subscriptionStatusResult, consumerPaidRead) = isSupportedOftFile.instructionMatches
        val subscriptionStatusRegister = subscriptionStatusResult.getInstruction<OneRegisterInstruction>().registerA
        val consumerPaid = consumerPaidRead.getInstruction<ReferenceInstruction>().reference

        isSupportedOftFile.method.apply {
            val afterStatusIndex = subscriptionStatusResult.index + 1
            val switchRegister = getFreeRegisterProvider(afterStatusIndex, 1, subscriptionStatusRegister)
                .getFreeRegister4Bit()
            addInstructionsWithLabels(
                afterStatusIndex,
                """
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->openEmailTemplates()Z
                    move-result v$switchRegister
                    if-eqz v$switchRegister, :check_subscription
                    sget-object v$subscriptionStatusRegister, $consumerPaid
                """,
                ExternalLabel("check_subscription", getInstruction(afterStatusIndex)),
            )
        }

        val openSupportedOftFile = OpenSupportedOftFileFingerprint.match()
        val (_, readingPaneCheck, readingPaneBranch) = openSupportedOftFile.instructionMatches
        openSupportedOftFile.method.apply {
            val readingPaneRegister = readingPaneCheck.getInstruction<TwoRegisterInstruction>().registerA
            addInstructionsWithLabels(
                readingPaneBranch.index,
                """
                    if-nez v$readingPaneRegister, :open_as_draft
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->openEmailTemplates()Z
                    move-result v$readingPaneRegister
                """,
                ExternalLabel("open_as_draft", getInstruction(readingPaneBranch.index)),
            )
        }
    }
}
