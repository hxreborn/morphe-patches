/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechrate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_SLIDER_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechRateSlider;"
private const val EXTENSION_TARGET_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechRateTarget;"

@Suppress("unused")
val speechRateSliderPatch = bytecodePatch(
    name = "Speech rate slider",
    description = "Adds a text-to-speech speed slider from 0.1x to 4x, saved per voice.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val applyRate = ApplySpeechRateFingerprint.method
        val rateField = applyRate.implementation!!.instructions
            .first { it.opcode == Opcode.IGET && it.getReference<FieldReference>()?.type == "F" }
            .getReference<FieldReference>()!!

        ApplySpeechRateFingerprint.classDef.apply {
            interfaces.add(EXTENSION_TARGET_DESCRIPTOR)
            addMethod(
                "applySpeechRate",
                listOf("F"),
                "V",
                AccessFlags.PUBLIC.value,
                3,
                """
                    iput p1, p0, $rateField
                    const/4 v0, 0x0
                    invoke-static { p0, v0 }, $applyRate
                    return-void
                """,
            )
        }

        SpeechRateRowFingerprint.let {
            val rootField = it.instructionMatches[0].instruction.getReference<FieldReference>()!!
            val seekBarId = it.method.getInstruction<WideLiteralInstruction>(it.instructionMatches[1].index).wideLiteral
            val labelId = it.method.getInstruction<WideLiteralInstruction>(it.instructionMatches[4].index).wideLiteral
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
                    invoke-static { v$rootRegister, v$seekBarRegister, v$labelRegister, p0 }, $EXTENSION_SLIDER_DESCRIPTOR->attach(Landroid/view/View;IILapp/hxreborn/extension/readera/SpeechRateTarget;)V
                """,
            )
        }

        SpeechRateViewFingerprint.let {
            val sliderVisibilityIndex = it.instructionMatches[0].index
            val upsellVisibilityIndex = it.instructionMatches[4].index
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
