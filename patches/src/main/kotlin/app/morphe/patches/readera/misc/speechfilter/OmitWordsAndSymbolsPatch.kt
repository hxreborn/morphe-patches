/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechfilter

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechFilterSettings;"

@Suppress("unused")
val omitWordsAndSymbolsPatch = bytecodePatch(
    name = "Omit words and symbols",
    description = "Makes Read aloud skip the words and symbols on a list, without changing the book. " +
        "The list is edited in the patches menu.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Omit words and symbols", "Read aloud skips these words and symbols. The book text is unchanged.")
        addSpeechTextFilter()
    }
}

internal fun BytecodePatchContext.addSpeechTextFilter() {
    val speakMatch = SpeakTextFingerprint.matchSingle()
    val contextField = speakMatch.classDef.fields.single { it.type == "Landroid/content/Context;" }
    val filterText = speakMatch.classDef.addMethod(
        "filterSpeechText", listOf("Ljava/lang/String;"), "Ljava/lang/String;", AccessFlags.PRIVATE.value, 3,
        """
            iget-object v0, p0, $contextField
            invoke-static {v0, p1}, $EXTENSION_CLASS_DESCRIPTOR->filter(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
            move-result-object v0
            return-object v0
        """,
    )
    val speechMatches = listOf(speakMatch) + SynthesizeTextFingerprint.matchAll(speakMatch.classDef, 0..1)
    speechMatches.forEach { speechMatch ->
        val (speechCall) = speechMatch.instructionMatches
        val textRegister = speechCall.getInstruction<FiveRegisterInstruction>().registerD
        speechMatch.method.addInstructions(
            speechCall.index,
            """
                invoke-direct {p0, v$textRegister}, ${descriptor(filterText)}
                move-result-object v$textRegister
            """,
        )
    }
}
