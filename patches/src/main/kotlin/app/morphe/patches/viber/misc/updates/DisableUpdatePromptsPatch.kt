/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableUpdatePromptsPatch: BytecodePatch = bytecodePatch(
    name = "Disable update prompts",
    description = "Stops the optional Google Play update dialog and the update request on links this version can't open. " +
        "Required updates still show.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("disableUpdatePrompts", disableUpdatePromptsPatch)
        val configType = UpdatePromptConfigFingerprint.matchSingle().classDef.type
        updatePromptCheckFingerprint(configType).matchSingle().apply {
            val (timedCheck, updateLinkCheck, _) = instructionMatches
            listOf(updateLinkCheck, timedCheck).forEach {
                method.addFilterCallAfter(it.index, "$VIBER_SETTINGS_CLASS->allowUpdatePrompt(Z)Z")
            }
        }
        UpdateRequiredDialogFingerprint.matchSingle().apply {
            val (_, _, labelsCall, returnDialog) = instructionMatches
            val labels = labelsCall.instruction.getReference<MethodReference>()!!
            val (_, messageSetter, positiveSetter, negativeSetter) = dialogLabelsFingerprint(labels).matchSingle()
                .instructionMatches.map { it.instruction.getReference<MethodReference>()!! }
            val (positiveLabelWrite) = positiveLabelFingerprint(positiveSetter).matchSingle().instructionMatches
            val positiveLabel = positiveLabelWrite.instruction.getReference<FieldReference>()!!
            val dialog = (returnDialog.instruction as OneRegisterInstruction).registerA
            val value = method.getFreeRegisterProvider(returnDialog.index, 1, dialog).getFreeRegister4Bit()
            method.addInstructionsWithLabels(
                returnDialog.index,
                """
                    const/4 v$value, 0x1
                    invoke-static {v$value}, $VIBER_SETTINGS_CLASS->allowUpdatePrompt(Z)Z
                    move-result v$value
                    if-nez v$value, :keep
                    const v$value, ${resourceId(ResourceType.STRING, "dialog_375_message")}
                    invoke-virtual {v$dialog, v$value}, ${descriptor(messageSetter)}
                    const v$value, ${resourceId(ResourceType.STRING, "dialog_button_ok")}
                    invoke-virtual {v$dialog, v$value}, ${descriptor(negativeSetter)}
                    const/4 v$value, 0x0
                    iput-object v$value, v$dialog, ${descriptor(positiveLabel)}
                """,
                ExternalLabel("keep", method.getInstruction(returnDialog.index)),
            )
        }
    }
}
