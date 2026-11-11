/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechfilter

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechFilterSettings;"

@Suppress("unused")
val speechTextFilterPatch = bytecodePatch(
    name = "Text-to-speech filter",
    description = "Makes Read aloud skip the words and symbols on a list, without changing the book. " +
        "The list is edited in the patches menu.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Text-to-speech filter", "Read aloud skips these words and symbols. The book text is unchanged.")
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

    }
}
