/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.footnotes

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SWITCH_TYPE = "Landroidx/appcompat/widget/SwitchCompat;"

@Suppress("unused")
val footnoteSpeechPatch = bytecodePatch(
    name = "Footnote speech",
    description = "Adds an option to read footnotes aloud during text-to-speech.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val prefsClass = TtsPrefsEventFingerprint.classDef.type
        val footnoteSwitch = TtsPrefsViewFingerprint.let { view ->
            val (upsellIndex, footnoteIndex) = view.instructionMatches.map { it.index }
            view.method.apply {
                val upsell = getInstruction<FiveRegisterInstruction>(upsellIndex)
                val footnoteRow = getInstruction<FiveRegisterInstruction>(footnoteIndex)
                replaceInstruction(
                    upsellIndex,
                    "invoke-virtual { v${upsell.registerC}, v${footnoteRow.registerD} }, Landroid/view/View;->setVisibility(I)V",
                )
                replaceInstruction(
                    footnoteIndex,
                    "invoke-virtual { v${footnoteRow.registerC}, v${upsell.registerD} }, Landroid/view/View;->setVisibility(I)V",
                )
            }
            val instructions = view.method.implementation!!.instructions.toList()
            instructions.indices.firstNotNullOf { index ->
                val field = instructions[index].takeIf { it.opcode == Opcode.IPUT_OBJECT }
                    ?.getReference<FieldReference>()?.takeIf { it.type == SWITCH_TYPE }
                val lookup = instructions.getOrNull(index - 3) as? FiveRegisterInstruction
                field.takeIf {
                    lookup?.opcode == Opcode.INVOKE_VIRTUAL &&
                        lookup.getReference<MethodReference>()?.name == "findViewById" &&
                        lookup.registerC == view.method.getInstruction<FiveRegisterInstruction>(
                            view.instructionMatches[1].index,
                        ).registerC
                }
            }
        }

        val toggle = ttsSwitchToggleFingerprint(prefsClass, footnoteSwitch)
        val speakFootnotes = toggle.instructionMatches[0].instruction.getReference<FieldReference>()!!
        val setter = toggle.instructionMatches[2].instruction.getReference<MethodReference>()!!
        val settingsGetter = toggle.method.getInstruction(toggle.instructionMatches[0].index - 2)
            .getReference<MethodReference>()!!
        check(settingsGetter.returnType == speakFootnotes.definingClass) { "Settings getter not found before $speakFootnotes" }

        val setterMatch = ttsFlagSetterFingerprint(setter.definingClass, setter.name)
        val preference = setterMatch.instructionMatches[2].instruction.getReference<FieldReference>()!!
        val sharedPreferences = setterMatch.method.getInstruction(setterMatch.instructionMatches[0].index - 1)
            .getReference<FieldReference>()!!

        ttsFlagInitFingerprint(speakFootnotes.definingClass, speakFootnotes).let { init ->
            init.method.apply {
                val readPreference = implementation!!.instructions.first {
                    it.opcode == Opcode.INVOKE_VIRTUAL &&
                        it.getReference<MethodReference>()?.let { method ->
                            method.definingClass == preference.type && method.returnType == "Ljava/lang/Boolean;"
                        } == true
                }.getReference<MethodReference>()!!
                val storeIndex = init.instructionMatches.single().index
                val store = getInstruction<TwoRegisterInstruction>(storeIndex)
                val registers = getFreeRegisterProvider(storeIndex, 2, store.registerA, store.registerB)
                val valueRegister = registers.getFreeRegister4Bit()
                val preferencesRegister = registers.getFreeRegister4Bit()
                replaceInstruction(storeIndex, "iput-boolean v$valueRegister, v${store.registerB}, $speakFootnotes")
                addInstructions(
                    storeIndex,
                    """
                        sget-object v$valueRegister, $preference
                        sget-object v$preferencesRegister, $sharedPreferences
                        invoke-virtual { v$valueRegister, v$preferencesRegister }, $readPreference
                        move-result-object v$valueRegister
                        invoke-virtual { v$valueRegister }, Ljava/lang/Boolean;->booleanValue()Z
                        move-result v$valueRegister
                    """,
                )
            }
        }

        val isFootnote = FootnoteUtteranceFingerprint.method
        skipFootnoteFingerprint(isFootnote.definingClass, isFootnote.name).method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructionsWithLabels(
                0,
                """
                    invoke-direct { p0, p1 }, $isFootnote
                    move-result p1
                    if-eqz p1, :skip
                    invoke-static { }, $settingsGetter
                    move-result-object p1
                    iget-boolean p1, p1, $speakFootnotes
                    xor-int/lit8 p1, p1, 0x1
                    :skip
                    return p1
                """,
            )
        }

        SpeakFootnoteFingerprint.let { speak ->
            val numberRegister = speak.method.getInstruction<OneRegisterInstruction>(speak.instructionMatches[1].index).registerA
            val textIndex = speak.instructionMatches[3].index + 1
            val registers = speak.method.getFreeRegisterProvider(textIndex, 2, numberRegister)
            val textRegister = registers.getFreeRegister4Bit()
            val flagRegister = registers.getFreeRegister4Bit()
            speak.method.addInstructionsWithLabels(
                textIndex,
                """
                    move-result-object v$textRegister
                    invoke-static { }, $settingsGetter
                    move-result-object v$flagRegister
                    iget-boolean v$flagRegister, v$flagRegister, $speakFootnotes
                    if-eqz v$flagRegister, :number
                    move-object v$numberRegister, v$textRegister
                    :number
                    nop
                """,
            )
        }
    }
}
