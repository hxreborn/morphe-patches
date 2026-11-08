/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechfilter

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.readera.shared.TtsPrefsViewFingerprint
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechFilterSettings;"

@Suppress("unused")
val speechTextFilterPatch = bytecodePatch(
    name = "Text-to-speech filter",
    description = "Adds \"Omit words and symbols\" to Settings > Text to speech (TTS). " +
        "Read aloud skips listed words, phrases and symbols without changing book text. " +
        "Includes bracket symbols by default.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val speakMatch = SpeakTextFingerprint.matchSingle()
        val speechMatches = listOf(speakMatch) + SynthesizeTextFingerprint.matchAll(speakMatch.classDef, 0..1)
        speechMatches.forEach { speechMatch ->
            val contextField = speechMatch.classDef.fields.single { it.type == "Landroid/content/Context;" }
            val (speechCall) = speechMatch.instructionMatches
            val textRegister = speechCall.getInstruction<FiveRegisterInstruction>().registerD
            val contextRegister = speechMatch.method.getFreeRegisterProvider(speechCall.index, 1, textRegister)
                .getFreeRegister4Bit()
            speechMatch.method.addInstructions(
                speechCall.index,
                """
                    iget-object v$contextRegister, p0, $contextField
                    invoke-static { v$contextRegister, v$textRegister }, $EXTENSION_CLASS_DESCRIPTOR->filter(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$textRegister
                """,
            )
        }

        TtsPrefsViewFingerprint.method.apply {
            implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_OBJECT }
                .reversed().forEach { (index, _) ->
                    val rootRegister = getInstruction<OneRegisterInstruction>(index).registerA
                    replaceInstruction(
                        index,
                        "invoke-static { v$rootRegister }, $EXTENSION_CLASS_DESCRIPTOR->attach(Landroid/view/View;)V",
                    )
                    addInstruction(index + 1, "return-object v$rootRegister")
                }
        }
    }
}
