/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.footnotes

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.readera.shared.TTS_FOOTNOTE_ROW
import app.morphe.patches.readera.shared.TTS_UPSELL_ROW
import app.morphe.patches.readera.shared.bindTtsPreference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.hideTtsPreferenceRow
import app.morphe.patches.readera.shared.showTtsPreferenceRow
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val footnoteSpeechPatch = bytecodePatch(
    name = "Footnote speech",
    description = "Adds an option to read footnotes aloud during text-to-speech.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val speakFootnotes = bindTtsPreference(TTS_FOOTNOTE_ROW)
        showTtsPreferenceRow(TTS_FOOTNOTE_ROW)
        hideTtsPreferenceRow(TTS_UPSELL_ROW)

        val isFootnote = FootnoteUtteranceFingerprint.method
        skipFootnoteFingerprint(isFootnote.definingClass, isFootnote.name).method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructionsWithLabels(
                0,
                """
                    invoke-direct { p0, p1 }, $isFootnote
                    move-result p1
                    if-eqz p1, :skip
                    invoke-static { }, ${speakFootnotes.settingsGetter}
                    move-result-object p1
                    iget-boolean p1, p1, ${speakFootnotes.flag}
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
                    invoke-static { }, ${speakFootnotes.settingsGetter}
                    move-result-object v$flagRegister
                    iget-boolean v$flagRegister, v$flagRegister, ${speakFootnotes.flag}
                    if-eqz v$flagRegister, :number
                    move-object v$numberRegister, v$textRegister
                    :number
                    nop
                """,
            )
        }
    }
}
