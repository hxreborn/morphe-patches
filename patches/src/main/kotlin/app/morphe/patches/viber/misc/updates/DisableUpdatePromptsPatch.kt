/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableUpdatePromptsPatch: Patch = patch(
    name = "Disable update prompts",
    description = "Stops the Google Play update dialog, the suggested and required upgrade dialogs and the update " +
        "request on links this version can't open. Viber's servers can still stop serving an old version.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("disableUpdatePrompts", disableUpdatePromptsPatch)
        val configType = UpdatePromptConfigFingerprint.matchSingle().classDef.type
        updatePromptCheckFingerprint(configType).matchSingle().apply {
            val (timedCheck, updateLinkCheck, _) = instructionMatches
            listOf(updateLinkCheck, timedCheck).forEach {
                val flag = (it.instruction as TwoRegisterInstruction).registerA
                method.replaceInstruction(it.index, "const/4 v$flag, 0x0")
            }
        }
        ClientUpgradeFingerprint.matchSingle().method.returnEarly()
        UnsupportedLinkDialogFingerprint.matchSingle().apply {
            val (_, _, labelsCall, returnDialog) = instructionMatches
            val labels = labelsCall.instruction.getReference<MethodReference>()!!
            val (_, messageSetter, positiveSetter, negativeSetter) = dialogLabelsFingerprint(labels).matchSingle()
                .instructionMatches.map { it.instruction.getReference<MethodReference>()!! }
            val (positiveLabelWrite) = positiveLabelFingerprint(positiveSetter).matchSingle().instructionMatches
            val positiveLabel = positiveLabelWrite.instruction.getReference<FieldReference>()!!
            val dialog = (returnDialog.instruction as OneRegisterInstruction).registerA
            val label = method.getFreeRegisterProvider(returnDialog.index, 1, dialog).getFreeRegister4Bit()
            method.addInstructions(
                returnDialog.index,
                """
                    const v$label, ${resourceId(ResourceType.STRING, "dialog_375_message")}
                    invoke-virtual {v$dialog, v$label}, ${descriptor(messageSetter)}
                    const v$label, ${resourceId(ResourceType.STRING, "dialog_button_ok")}
                    invoke-virtual {v$dialog, v$label}, ${descriptor(negativeSetter)}
                    const/4 v$label, 0x0
                    iput-object v$label, v$dialog, ${descriptor(positiveLabel)}
                """,
            )
        }
    }
}
