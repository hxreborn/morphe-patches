/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechrate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.addPatchInterface
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import app.morphe.patches.shared.misc.extension.readeraSpeechRateSliderExtensionPatch

private const val EXTENSION_SLIDER_DESCRIPTOR = "Lapp/hxreborn/extension/readera/speech/rate/SpeechRateSlider;"
private const val EXTENSION_TARGET_DESCRIPTOR = "Lapp/hxreborn/extension/readera/speech/rate/SpeechRateTarget;"

@Suppress("unused")
val speechRateSliderPatch = patch(
    name = "Speech rate slider",
    description = "Sets Read aloud speed from 0.1x to 4x with a slider on the Speech rate screen.",
) {
    dependsOn(readeraSpeechRateSliderExtensionPatch)
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Speech rate slider", "Sets Read aloud speed from 0.1x to 4x with a slider.")
        val applyRate = ApplySpeechRateFingerprint.method
        val rateField = applyRate.implementation!!.instructions
            .first { it.opcode == Opcode.IGET && it.getReference<FieldReference>()?.type == "F" }
            .getReference<FieldReference>()!!

        ApplySpeechRateFingerprint.classDef.apply {
            addPatchInterface(
                EXTENSION_TARGET_DESCRIPTOR,
                "applySpeechRate",
                """
                    iput p1, p0, $rateField
                    const/4 v0, 0x0
                    invoke-static { p0, v0 }, $applyRate
                    return-void
                """,
            )
        }

        SpeechRateRowFingerprint.let {
            val rootField = it.instructionMatches[SPEECH_RATE_ROOT_MATCH].instruction.getReference<FieldReference>()!!
            val seekBarId = it.method.getInstruction<WideLiteralInstruction>(
                it.instructionMatches[SPEECH_RATE_SEEK_BAR_ID_MATCH].index,
            ).wideLiteral
            val labelId = it.method.getInstruction<WideLiteralInstruction>(
                it.instructionMatches[SPEECH_RATE_LABEL_ID_MATCH].index,
            ).wideLiteral
            val returnIndex = it.method.indexOfFirstInstructionReversed(Opcode.RETURN_VOID)
            val registers = it.method.getFreeRegisterProvider(returnIndex, 3)
            val rootRegister = registers.getFreeRegister4Bit()
            val seekBarRegister = registers.getFreeRegister4Bit()
            val labelRegister = registers.getFreeRegister4Bit()

            it.method.addInstructions(
                returnIndex,
                """
                    iget-object v$rootRegister, p0, $rootField
                    const v$seekBarRegister, $seekBarId
                    const v$labelRegister, $labelId
                    invoke-static { v$rootRegister, v$seekBarRegister, v$labelRegister, p0 }, $EXTENSION_SLIDER_DESCRIPTOR->attach(Landroid/view/View;IILapp/hxreborn/extension/readera/speech/rate/SpeechRateTarget;)V
                """,
            )
        }

        SpeechRateViewFingerprint.let {
            val (sliderGone, _, _, _, upsellVisibilityCall) = it.instructionMatches
            val sliderVisibilityIndex = sliderGone.index
            val upsellVisibilityIndex = upsellVisibilityCall.index
            it.method.apply {
                val upsellRegister = getInstruction<FiveRegisterInstruction>(upsellVisibilityIndex).registerC
                val goneRegister = getFreeRegisterProvider(upsellVisibilityIndex, 1, upsellRegister).getFreeRegister4Bit()
                replaceInstruction(
                    upsellVisibilityIndex,
                    "invoke-virtual { v$upsellRegister, v$goneRegister }, Landroid/view/View;->setVisibility(I)V",
                )
                addInstruction(upsellVisibilityIndex, "const/16 v$goneRegister, 0x8")

                val sliderRegister = getInstruction<OneRegisterInstruction>(sliderVisibilityIndex).registerA
                replaceInstruction(sliderVisibilityIndex, "const/4 v$sliderRegister, 0x0")
            }
        }
    }
}
