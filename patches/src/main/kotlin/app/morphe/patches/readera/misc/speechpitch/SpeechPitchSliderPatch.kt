/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechpitch

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.misc.materialcontrols.MATERIAL_RADIO_BUTTON_TAG
import app.morphe.patches.readera.misc.speechfilter.SpeakTextFingerprint
import app.morphe.patches.readera.misc.speechfilter.SynthesizeTextFingerprint
import app.morphe.patches.readera.shared.TtsPrefsEventFingerprint
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.asSequence
import app.morphe.util.childElementsSequence
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechPitchSlider;"
private const val EXTENSION_TARGET_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechPitchTarget;"
private const val PITCH_LAYOUT = "res/layout/hx_readera_pitch.xml"
private val radioButtonTags = setOf("RadioButton", MATERIAL_RADIO_BUTTON_TAG)
private lateinit var pitchResources: ResourcePatchContext

private val pitchResourcesPatch = resourcePatch {
    execute {
        pitchResources = this
        decode("res/layout/*.xml")
        val sliderLayout = get("res/layout").listFiles()!!.filter { it.extension == "xml" }.single { file ->
            file.inputStream().use { document(it) }.use { document ->
                val root = document.documentElement
                root.tagName == "LinearLayout" &&
                    root.childElementsSequence().any { it.tagName in radioButtonTags } &&
                    root.getElementsByTagName("SeekBar").asSequence().any {
                        (it as Element).getAttribute("android:max") == "400"
                    }
            }
        }
        get(PITCH_LAYOUT).writeText(sliderLayout.readText())
        document(PITCH_LAYOUT).use { document ->
            val root = document.documentElement
            root.removeAttribute("android:id")
            root.setAttribute("android:tag", "hx_readera_pitch")
            root.setAttribute("android:clickable", "false")
            root.setAttribute("android:focusable", "false")
            root.removeAttribute("android:paddingLeft")
            root.removeAttribute("android:paddingStart")
            val radio = root.getElementsByTagName("*").asSequence().first { (it as Element).tagName in radioButtonTags }
            radio.parentNode.removeChild(radio)
            root.getElementsByTagName("*").asSequence().filterIsInstance<Element>().forEach {
                it.removeAttribute("android:id")
            }
            val (title, value) = root.getElementsByTagName("TextView").asSequence().map { it as Element }.toList()
            title.setAttribute("android:text", "Speech pitch")
            value.setAttribute("android:id", "@android:id/text2")
            value.setAttribute("android:text", "1.0x")
            (root.getElementsByTagName("SeekBar").item(0) as Element).apply {
                setAttribute("android:id", "@android:id/progress")
                removeAttribute("android:min")
                setAttribute("android:max", "150")
                setAttribute("android:progress", "50")
                setAttribute("android:contentDescription", "Speech pitch")
                setAttribute("android:minHeight", "48dp")
            }
        }
    }
}

@Suppress("unused")
val speechPitchSliderPatch = bytecodePatch(
    name = "Speech pitch slider",
    description = "Adds a voice pitch slider from 0.5x to 2x to Settings > Text to speech (TTS).",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, pitchResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Speech pitch slider", "Adds a voice pitch slider from 0.5x to 2x to Settings > Text to speech (TTS).")
        val view = PitchPreferencesViewFingerprint.matchAll(1..1).single()
        val (_, _, rowLoad) = view.instructionMatches
        val rowId = (rowLoad.instruction as WideLiteralInstruction).wideLiteral
        val rowName = pitchResources.document("res/values/public.xml").use { document ->
            document.getElementsByTagName("public").asSequence().map { it as Element }.single {
                it.getAttribute("type") == "id" && it.getAttribute("id").removePrefix("0x").toLong(16) == rowId
            }.getAttribute("name")
        }
        val screen = pitchResources.get("res/layout").listFiles()!!.filter { it.extension == "xml" }.single { file ->
            file.inputStream().use { pitchResources.document(it) }.use { document ->
                document.getElementsByTagName("include").asSequence().any {
                    (it as Element).getAttribute("android:id") == "@id/$rowName"
                }
            }
        }
        pitchResources.document("res/layout/./${screen.name}").use { document ->
            val container = document.documentElement.childElementsSequence().single()
            val row = document.createElement("include")
            row.setAttribute("layout", "@layout/hx_readera_pitch")
            container.appendChild(row)
        }

        val (_, summaryRead) = SpeechRateSummaryFingerprint.matchAll(1..1).single().instructionMatches
        val summaryField = summaryRead.instruction.getReference<FieldReference>()!!
        val rateRow = speechRateRowFingerprint(summaryField).matchAll(1..1).single()
        val (summaryLookup) = rateRow.instructionMatches
        val rateRowRegister = summaryLookup.getInstruction<FiveRegisterInstruction>().registerC
        val rateRowLoad = rateRow.method.indexOfFirstInstructionReversedOrThrow(summaryLookup.index) {
            opcode == Opcode.CONST && (this as OneRegisterInstruction).registerA == rateRowRegister
        }
        val rateRowId = rateRow.method.getInstruction<WideLiteralInstruction>(rateRowLoad).wideLiteral
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceStub(
            "speechRateRowId", 1,
            """
                const v0, $rateRowId
                return v0
            """,
        )

        val selection = RateVoiceSelectionFingerprint.matchAll(1..1).single()
        val (languageCall, _, _, voiceCall) = selection.instructionMatches
        val language = languageCall.instruction.getReference<MethodReference>()!!
        val voice = voiceCall.instruction.getReference<MethodReference>()!!
        val selectedVoice = selectedVoiceFingerprint(voice.returnType).matchAll(1..1).single().method
        view.classDef.apply {
            interfaces.add(EXTENSION_TARGET_DESCRIPTOR)
            addMethod(
                "speechPitchVoice", emptyList(), "Landroid/speech/tts/Voice;", AccessFlags.PUBLIC.value, 2,
                """
                    invoke-virtual {p0}, $language
                    move-result-object v0
                    invoke-virtual {p0, v0}, $voice
                    move-result-object v0
                    if-eqz v0, :default_voice
                    invoke-virtual {v0}, $selectedVoice
                    move-result-object v0
                    return-object v0
                    :default_voice
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
        }
        val refreshMethods = listOf(
            PitchPreferencesResumeFingerprint.matchAll(1..1).single().method,
            TtsPrefsEventFingerprint.matchAll(1..1).single().method,
        )
        refreshMethods.forEach { method ->
            val returnIndex = method.indexOfFirstInstructionReversed(Opcode.RETURN_VOID)
            val rootRegister = method.getFreeRegisterProvider(returnIndex, 1).getFreeRegister4Bit()
            method.addInstructions(
                returnIndex,
                """
                    invoke-virtual {p0}, Landroid/app/Fragment;->getView()Landroid/view/View;
                    move-result-object v$rootRegister
                    invoke-static {v$rootRegister, p0}, $EXTENSION_CLASS_DESCRIPTOR->attach(Landroid/view/View;Lapp/hxreborn/extension/readera/SpeechPitchTarget;)V
                """,
            )
        }

        val speak = SpeakTextFingerprint.matchAll(1..1).single()
        val speechVoice = speak.classDef.fields.single { it.type == voice.returnType }
        speak.classDef.apply {
            interfaces.add(EXTENSION_TARGET_DESCRIPTOR)
            addMethod(
                "speechPitchVoice", emptyList(), "Landroid/speech/tts/Voice;", AccessFlags.PUBLIC.value, 2,
                """
                    iget-object v0, p0, $speechVoice
                    if-eqz v0, :default_voice
                    invoke-virtual {v0}, $selectedVoice
                    move-result-object v0
                    return-object v0
                    :default_voice
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
        }
        val contextField = speak.classDef.fields.single { it.type == "Landroid/content/Context;" }
        val applyPitch = speak.classDef.addMethod(
            "applySpeechPitch", listOf("Landroid/speech/tts/TextToSpeech;"), "V", AccessFlags.PRIVATE.value, 3,
            """
                iget-object v0, p0, $contextField
                invoke-static {v0, p1, p0}, $EXTENSION_CLASS_DESCRIPTOR->applyPitch(Landroid/content/Context;Landroid/speech/tts/TextToSpeech;Lapp/hxreborn/extension/readera/SpeechPitchTarget;)V
                return-void
            """,
        )
        val requests = listOf(speak) + SynthesizeTextFingerprint.matchAll(speak.classDef, 0..1)
        requests.forEach { request ->
            val (speechCall) = request.instructionMatches
            val ttsRegister = speechCall.getInstruction<FiveRegisterInstruction>().registerC
            request.method.addInstructions(
                speechCall.index,
                """
                    invoke-direct {p0, v$ttsRegister}, $applyPitch
                """,
            )
        }
    }
}
