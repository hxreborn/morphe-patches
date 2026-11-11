/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.chapterspeech

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.readera.shared.CHAPTER_READER
import app.morphe.patches.readera.shared.ReadAloudFromCurrentPositionFingerprint
import app.morphe.patches.readera.shared.chapterReaderPatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.readera.shared.stringNamesByEnglishText
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.parameterRegister
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val BOUNDARY_RESOLVER = "Lapp/hxreborn/extension/readera/SpeechBoundaryResolver;"
private const val CONTENTS_PANEL_CHAIN_LENGTH = 2
private const val CHAPTER_SPEECH = "Lapp/hxreborn/extension/readera/ChapterSpeech;"
private const val READING_POSITION = "Lapp/hxreborn/extension/readera/ReadingPosition;"
private const val CONTENTS_ENTRY = "Lapp/hxreborn/extension/readera/ContentsEntry;"
private const val SPEECH_TRACK = "Lapp/hxreborn/extension/readera/SpeechTrack;"
private const val CONTENTS_ROW = "Lapp/hxreborn/extension/readera/ContentsRow;"
private const val CONTENTS_PANEL = "Lapp/hxreborn/extension/readera/ContentsPanel;"

private const val EMPTY_CONTENTS_MESSAGE = "Table of Contents is not available for this document"

private var emptyContentsMessage = -1L
private val contentsMessageResourcePatch = resourcePatch {
    execute {
        val name = stringNamesByEnglishText()[EMPTY_CONTENTS_MESSAGE]?.first()
            ?: throw PatchException("No string \"$EMPTY_CONTENTS_MESSAGE\" in res/values/strings.xml")
        emptyContentsMessage = resourceId(ResourceType.STRING, name)
    }
}

@Suppress("unused")
val readChapterAloudPatch = bytecodePatch(
    name = "Read chapter aloud",
    description = "Reads one chapter aloud and stops at its end. " +
        "Long-press the chapter in Contents to start.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, chapterReaderPatch, contentsMessageResourcePatch)
    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Read chapter aloud", "Long-press a chapter in Contents to read only that chapter aloud.")
        val click = ContentsRowClickFingerprint.matchAll(1..1).single()
        val clickMatches = click.instructionMatches
        val (payloadCall, _, entryCast, activityRead, activityCall) = clickMatches
        val (_, _, closeCall) = clickMatches.subList(clickMatches.indexOf(activityCall) + 1, clickMatches.size)
        val nodeRead = click.method.getInstruction<TwoRegisterInstruction>(payloadCall.index - 1)
        val nodeField = nodeRead.getReference<FieldReference>()!!
        val payload = payloadCall.instruction.getReference<MethodReference>()!!
        val entryType = entryCast.instruction.getReference<TypeReference>()!!.type
        val holderType = click.originalMethod.parameterTypes.first().toString()
        val chainReads = click.method.implementation!!.instructions.toList()
            .subList(0, activityRead.index)
            .filter { it.opcode == Opcode.IGET_OBJECT }.takeLast(CONTENTS_PANEL_CHAIN_LENGTH)
        val (holderRead, panelRead) = chainReads.map { it as TwoRegisterInstruction }
        val holderField = holderRead.getReference<FieldReference>()!!
        val panelField = panelRead.getReference<FieldReference>()!!
        val activityField = activityRead.instruction.getReference<FieldReference>()!!
        val close = closeCall.instruction.getReference<MethodReference>()!!
        val holderParameter = click.method.implementation!!.registerCount - click.originalMethod.parameterTypes.size
        check(holderRead.registerB == holderParameter && holderRead.registerA == panelRead.registerB &&
            panelRead.registerA == (activityRead.instruction as TwoRegisterInstruction).registerB) {
            "Contents panel chain disagrees: ${descriptor(holderField)}, ${descriptor(panelField)}, ${descriptor(activityField)}"
        }
        check(holderField.definingClass == holderType && holderField.type == panelField.definingClass) {
            "Contents holder types disagree: ${descriptor(holderField)}, ${descriptor(panelField)}"
        }

        val entryClass = classDefBy(entryType)
        val positionType = entryClass.superclass!!
        val entryConstructor = entryClass.methods.single {
            it.name == "<init>" && it.parameterTypes == listOf("I", "I", "Ljava/lang/String;", "I")
        }
        val linkConstructor = entryClass.methods.single {
            it.name == "<init>" && it.parameterTypes == listOf("Ljava/lang/String;")
        }
        val page = entryConstructor.parameterField(0)
        val pagesCount = entryConstructor.parameterField(1)
        val positionKind = classDefBy(positionType).methods.single {
            it.name == "<init>" && it.parameterTypes == listOf("I")
        }.parameterField(0)
        val title = entryConstructor.parameterField(entryConstructor.parameterTypes.indexOf("Ljava/lang/String;"))
        val level = entryConstructor.parameterField(entryConstructor.parameterTypes.lastIndex)
        val link = linkConstructor.parameterField(0)
        val ratio = entryConstructor.implementation!!.instructions.single { it.opcode == Opcode.IPUT_WIDE }
            .getReference<FieldReference>()!!
        check(page.definingClass == positionType && ratio.definingClass == positionType) {
            "Contents position types disagree: $positionType, ${descriptor(page)}, ${descriptor(ratio)}"
        }

        val toolbar = ReadAloudFromCurrentPositionFingerprint.matchAll(1..1).single()
        val (_, documentRead, positionRead, copyCall) = toolbar.instructionMatches
        val document = documentRead.instruction.getReference<FieldReference>()!!
        val visiblePosition = positionRead.instruction.getReference<FieldReference>()!!
        val copy = copyCall.instruction.getReference<MethodReference>()!!
        check(copy.definingClass == positionType) {
            "Reading position types disagree: $positionType, ${descriptor(copy)}"
        }

        val start = SpeechStartTaskFingerprint.matchAll(1..1).single()
        val startMatches = start.instructionMatches
        val (restartRead, _, taskDocumentRead, taskPositionRead, visibleCopyCall) = startMatches
        val (codecRead, requestedRead, _, generationRead) = startMatches.subList(
            startMatches.indexOf(visibleCopyCall) + 1, startMatches.size,
        )
        val codec = codecRead.instruction.getReference<FieldReference>()!!
        val requested = requestedRead.instruction.getReference<FieldReference>()!!
        val taskCopy = visibleCopyCall.instruction.getReference<MethodReference>()!!
        val taskDocument = taskDocumentRead.instruction.getReference<FieldReference>()!!
        val taskPosition = taskPositionRead.instruction.getReference<FieldReference>()!!
        check(taskCopy == copy && taskPosition == visiblePosition && taskDocument.type == document.type && requested.type == positionType) {
            "Speech start types disagree: ${descriptor(taskCopy)}, ${descriptor(copy)}, ${descriptor(taskPosition)}, ${descriptor(visiblePosition)}, ${descriptor(taskDocument)}, ${descriptor(document)}, ${descriptor(requested)}"
        }
        val utteranceConversion = start.method.implementation!!.instructions.toList()
            .subList(generationRead.index + 1, start.method.implementation!!.instructions.count())
            .single { instruction ->
                val call = instruction.getReference<MethodReference>()
                instruction.opcode == Opcode.INVOKE_INTERFACE && call?.parameterTypes == listOf(positionType) && call.returnType.startsWith("L")
            }.getReference<MethodReference>()!!
        check(utteranceConversion.definingClass == codec.type) {
            "Speech codec types disagree: ${descriptor(codec)}, ${descriptor(utteranceConversion)}"
        }
        val tree = ContentsTreeFingerprint.matchAll(1..1).single()
        val contentsEvents = contentsTreeEventFingerprint(tree.originalMethod.definingClass).matchAll(2..2)
        val contentsGetter = contentsEvents.map {
            val (getter, _, _, _) = it.instructionMatches
            descriptor(getter.instruction.getReference<MethodReference>()!!)
        }.distinct().single()
        check(contentsGetter.startsWith("${codec.type}->")) {
            "Contents codec types disagree: ${descriptor(codec)}, $contentsGetter"
        }

        val next = NextTrackFingerprint.matchAll(1..1).single()
        val (_, trackCall, trackResult, _) = next.instructionMatches
        val trackType = trackCall.instruction.getReference<MethodReference>()!!.returnType
        val spoken = spokenPositionFingerprint(trackType).matchAll(1..1).single()
        val (spokenRead, spokenTypeCheck) = spoken.instructionMatches
        val spokenPosition = spokenRead.instruction.getReference<FieldReference>()!!
        val spokenType = spokenTypeCheck.instruction.getReference<TypeReference>()!!.type
        check(spokenType == positionType) { "Spoken position types disagree: $spokenType, $positionType" }
        val trackGeneration = classDefBy(trackType).fields.single { it.type == "I" }
        val relayout = speechRelayoutFingerprint(positionType).matchAll(1..1).single()
        val (layoutRead, _, currentLayoutCall) = relayout.instructionMatches
        val layoutHash = layoutRead.instruction.getReference<FieldReference>()!!
        val decode = relayout.instructionMatches[SPEECH_RELAYOUT_DECODE_MATCH].instruction.getReference<MethodReference>()!!
        check(currentLayoutCall.instruction.getReference<MethodReference>()!!.definingClass == codec.type && decode.definingClass == codec.type) {
            "Speech relayout types disagree: ${descriptor(codec)}, ${descriptor(decode)}"
        }
        val hasAnchor = positionDecoderFingerprint(positionType, decode.name).matchAll(2..2).map {
            val (anchorCall) = it.instructionMatches
            descriptor(anchorCall.instruction.getReference<MethodReference>()!!)
        }.distinct().single()

        val restart = restartRead.instruction.getReference<FieldReference>()!!
        val generation = generationRead.instruction.getReference<FieldReference>()!!
        val pageSpeech = pageSpeechFingerprint(start.originalMethod.definingClass).matchAll(1..1).single()
        val pageMatches = pageSpeech.instructionMatches
        val (pagesRead, pagesCountRead, _, pageArrayRead) = pageMatches
        val (cachedSpeechCall, _, _, pageCodecRead, buildSpeechCall) = pageMatches.subList(
            pageMatches.indexOf(pageArrayRead) + 2, pageMatches.size,
        )
        check(pageCodecRead.instruction.getReference<FieldReference>() == codec) {
            "Page speech codec disagrees: ${descriptor(pageCodecRead.instruction.getReference<FieldReference>()!!)}, ${descriptor(codec)}"
        }
        val pause = SpeechFinishPauseFingerprint.matchAll(1..1).single()
        val (finishRead, setReason, callbackRead, pauseCall) = pause.instructionMatches
        val serviceType = next.originalMethod.definingClass
        val first = firstSpeechTrackFingerprint(serviceType, trackType).matchAll(1..1).single()
        val (metadataCall) = first.instructionMatches
        val metadata = metadataCall.instruction as FiveRegisterInstruction
        val callbackService = classDefBy(first.originalMethod.definingClass).fields.single { it.type == serviceType }
        val utteranceType = utteranceConversion.returnType
        val lookAheads = queuedSpeechLookAheadFingerprint(utteranceType).matchAll(0..1).map { lookAhead ->
            lookAhead to classDefBy(lookAhead.originalMethod.definingClass).fields.single { it.type == "I" }
        }
        val directSpeaks = speakDirectFallbackFingerprint(utteranceType).matchAll(0..1).map { fallback ->
            val (speakCall) = fallback.instructionMatches
            val accessor = speakCall.instruction.getReference<MethodReference>()!!
            val directSpeak = classDefBy(accessor.definingClass).methods.single { descriptor(it) == descriptor(accessor) }
                .implementation!!.instructions.single { it.opcode == Opcode.INVOKE_DIRECT }.getReference<MethodReference>()!!
            val player = mutableClassDefBy(accessor.definingClass)
            val speak = player.methods.single { descriptor(it) == descriptor(directSpeak) }
            check(speak.parameterTypes == listOf(utteranceType) && speak.implementation!!.registerCount > speak.parameterTypes.size + 1) {
                "Direct speech types disagree: ${descriptor(directSpeak)}, $utteranceType"
            }
            speak to player.fields.single { it.type == "I" }
        }
        check(lookAheads.size + directSpeaks.size == 1) {
            "Queued speech sites disagree: ${lookAheads.size} look-ahead, ${directSpeaks.size} fallback"
        }
        val skip = SkipToNextFingerprint.matchAll(1..1).single()
        val manualCall = skip.method.implementation!!.instructions.withIndex().single { (_, instruction) ->
            val reference = instruction.getReference<MethodReference>()
            instruction.opcode == Opcode.INVOKE_STATIC && reference?.parameterTypes == listOf(serviceType) && reference.returnType == "V"
        }
        val advanceReference = manualCall.value.getReference<MethodReference>()!!
        val advanceCalls = classDefBy(serviceType).methods.single { descriptor(it) == descriptor(advanceReference) }
            .implementation!!.instructions.mapNotNull { it.getReference<MethodReference>() }
        check(advanceCalls == listOf(next.originalMethod)) {
            "Manual speech advance disagrees: ${descriptor(advanceReference)}, ${descriptor(next.originalMethod)}"
        }
        val rowInit = ContentsRowInitFingerprint.matchAll(1..1).single()
        val (rowClickListener) = rowInit.instructionMatches
        val placeholder = contentsPlaceholderFingerprint(panelField.type, emptyContentsMessage).matchAll(1..1).single()
        val (placeholderField, messageConstant, placeholderText) = placeholder.instructionMatches
        val placeholderCall = placeholderText.instruction as FiveRegisterInstruction
        val placeholderRegister = placeholderCall.registerC
        check((placeholderField.instruction as TwoRegisterInstruction).registerA == placeholderRegister &&
            (messageConstant.instruction as OneRegisterInstruction).registerA == placeholderCall.registerD) {
            "Contents message assignment disagrees: ${descriptor(placeholderField.instruction.getReference<FieldReference>()!!)}"
        }

        mutableClassDefBy(positionType).apply {
            interfaces.add(READING_POSITION)
            addReader("readingPage", page)
            addReader("readingPagesCount", pagesCount)
            addReader("readingType", positionKind)
            addReader("readingLayoutHash", layoutHash)
            addReader("readingRatio", ratio)
        }
        mutableClassDefBy(entryType).apply {
            interfaces.add(CONTENTS_ENTRY)
            addReader("contentsLevel", level)
            addReader("contentsTitle", title)
            addReader("contentsLink", link)
        }
        mutableClassDefBy(trackType).apply {
            interfaces.add(SPEECH_TRACK)
            addReader("speechGeneration", trackGeneration)
            addReader("spokenPosition", spokenPosition)
        }
        mutableClassDefBy(holderType).apply {
            interfaces.add(CONTENTS_ROW)
            addMethod("contentsEntry", emptyList(), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 2, """
                iget-object v0, p0, ${descriptor(nodeField)}
                invoke-virtual {v0}, ${descriptor(payload)}
                move-result-object v0
                return-object v0
            """)
            addMethod("openContentsEntry", listOf("Landroid/view/View;"), "V", AccessFlags.PUBLIC.value, 2, """
                invoke-static {p0, p1}, ${descriptor(click.originalMethod)}
                return-void
            """)
            addMethod("contentsPanel", emptyList(), CONTENTS_PANEL, AccessFlags.PUBLIC.value, 2, """
                iget-object v0, p0, ${descriptor(holderField)}
                iget-object v0, v0, ${descriptor(panelField)}
                return-object v0
            """)
        }
        mutableClassDefBy(panelField.type).apply {
            interfaces.add(CONTENTS_PANEL)
            addMethod("contentsReader", emptyList(), CHAPTER_READER, AccessFlags.PUBLIC.value, 2, """
                iget-object v0, p0, ${descriptor(activityField)}
                return-object v0
            """)
            addMethod("closeContents", emptyList(), "V", AccessFlags.PUBLIC.value, 1, """
                invoke-virtual {p0}, ${descriptor(close)}
                return-void
            """)
        }
        val task = mutableClassDefBy(start.originalMethod.definingClass)
        task.interfaces.add(BOUNDARY_RESOLVER)
        task.addMethod("contents", emptyList(), "Ljava/util/List;", AccessFlags.PUBLIC.value, 2, """
            iget-object v0, p0, ${descriptor(codec)}
            invoke-interface {v0}, $contentsGetter
            move-result-object v0
            return-object v0
        """)
        task.addMethod("speechStart", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 5, """
            check-cast p1, $positionType
            new-instance v0, $positionType
            invoke-direct {v0, p1}, ${descriptor(copy)}
            iget-object v1, p0, ${descriptor(codec)}
            invoke-virtual {v0}, $hasAnchor
            move-result v2
            if-eqz v2, :resolve
            invoke-interface {v1, v0}, ${descriptor(decode)}
            :resolve
            invoke-interface {v1, v0}, ${descriptor(utteranceConversion)}
            move-result-object v0
            return-object v0
        """)
        task.addMethod("pageSpeech", listOf("I"), "Ljava/util/List;", AccessFlags.PUBLIC.value, 4, """
            iget-object v0, p0, ${descriptor(pagesRead.instruction.getReference<FieldReference>()!!)}
            iget v1, v0, ${descriptor(pagesCountRead.instruction.getReference<FieldReference>()!!)}
            if-ltz p1, :missing
            if-ge p1, v1, :missing
            iget-object v0, v0, ${descriptor(pageArrayRead.instruction.getReference<FieldReference>()!!)}
            aget-object v0, v0, p1
            invoke-virtual {v0}, ${descriptor(cachedSpeechCall.instruction.getReference<MethodReference>()!!)}
            move-result-object v1
            if-nez v1, :found
            iget-object v1, p0, ${descriptor(codec)}
            invoke-interface {v1, v0}, ${descriptor(buildSpeechCall.instruction.getReference<MethodReference>()!!)}
            move-result-object v1
            :found
            return-object v1
            :missing
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
        """)
        val resolveStart = task.addMethod("chapterStartPosition", listOf(positionType), positionType, AccessFlags.PRIVATE.value, 6, """
            iget-object v0, p0, ${descriptor(requested)}
            iget-boolean v1, p0, ${descriptor(restart)}
            iget v2, p0, ${descriptor(generation)}
            invoke-static {p0, v0, v1, v2, p1}, $CHAPTER_SPEECH->startPosition(${BOUNDARY_RESOLVER}Ljava/lang/Object;ZILjava/lang/Object;)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, $positionType
            return-object v0
        """)
        val visibleCopy = visibleCopyCall.instruction as FiveRegisterInstruction
        val taskRegister = (requestedRead.instruction as TwoRegisterInstruction).registerB
        start.method.addInstructions(visibleCopyCall.index + 1, """
            invoke-direct {v$taskRegister, v${visibleCopy.registerC}}, ${descriptor(resolveStart)}
            move-result-object v${visibleCopy.registerC}
        """)
        val trackRegister = (trackResult.instruction as OneRegisterInstruction).registerA
        next.method.addInstructions(trackResult.index + 1, """
            invoke-static {v$trackRegister}, $CHAPTER_SPEECH->nextTrack(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$trackRegister
            check-cast v$trackRegister, $trackType
        """)
        val service = mutableClassDefBy(serviceType)
        service.addMethod("pauseAtChapterEnd", emptyList(), "V", AccessFlags.PUBLIC.value, 2, """
            sget-object v0, ${descriptor(finishRead.instruction.getReference<FieldReference>()!!)}
            invoke-direct {p0, v0}, ${descriptor(setReason.instruction.getReference<MethodReference>()!!)}
            iget-object v0, p0, ${descriptor(callbackRead.instruction.getReference<FieldReference>()!!)}
            invoke-virtual {v0}, ${descriptor(pauseCall.instruction.getReference<MethodReference>()!!)}
            return-void
        """)
        first.method.addInstructionsWithLabels(metadataCall.index + 1, """
            invoke-static {v${metadata.registerD}}, $CHAPTER_SPEECH->playsFirstTrack(Ljava/lang/Object;)Z
            move-result v${metadata.registerC}
            if-nez v${metadata.registerC}, :chapter_play
            iget-object v${metadata.registerC}, p0, ${descriptor(callbackService)}
            invoke-virtual {v${metadata.registerC}}, ${service.type}->pauseAtChapterEnd()V
            return-void
        """, ExternalLabel("chapter_play", first.method.getInstruction(metadataCall.index + 1)))
        lookAheads.forEach { (lookAhead, playerGeneration) ->
            val (nextPositionRead, _, queuedSetRead, _) = lookAhead.instructionMatches
            val positionRegister = (nextPositionRead.instruction as TwoRegisterInstruction).registerA
            val generationRegister = (queuedSetRead.instruction as TwoRegisterInstruction).registerA
            lookAhead.method.addInstructions(nextPositionRead.index + 1, """
                iget v$generationRegister, p0, ${descriptor(playerGeneration)}
                invoke-static {v$positionRegister, v$generationRegister}, $CHAPTER_SPEECH->lookAhead(Ljava/lang/Object;I)Ljava/lang/Object;
                move-result-object v$positionRegister
                check-cast v$positionRegister, $utteranceType
            """)
        }
        directSpeaks.forEach { (speak, playerGeneration) ->
            speak.addInstructionsWithLabels(0, """
                iget v0, p0, ${descriptor(playerGeneration)}
                invoke-static {p1, v0}, $CHAPTER_SPEECH->lookAhead(Ljava/lang/Object;I)Ljava/lang/Object;
                move-result-object v0
                if-nez v0, :chapter_speak
                return-void
            """, ExternalLabel("chapter_speak", speak.getInstruction(0)))
        }
        skip.method.addInstructions(manualCall.index + 1, "invoke-static {}, $CHAPTER_SPEECH->endManualAdvance()V")
        skip.method.addInstructions(manualCall.index, "invoke-static {}, $CHAPTER_SPEECH->beginManualAdvance()V")
        val rowRegister = (rowClickListener.instruction as FiveRegisterInstruction).registerC
        rowInit.method.addInstructions(rowClickListener.index + 1, """
            invoke-static {v$rowRegister, p0}, $CHAPTER_SPEECH->attachContentsRow(Landroid/view/View;Lapp/hxreborn/extension/readera/ContentsRow;)V
        """)
        placeholder.method.addInstructions(placeholderText.index + 1, """
            invoke-static {v$placeholderRegister, p0}, $CHAPTER_SPEECH->attachContentsPlaceholder(Landroid/widget/TextView;Lapp/hxreborn/extension/readera/ContentsPanel;)V
        """)
    }
}

private fun Method.parameterField(index: Int): FieldReference {
    val register = implementation!!.registerCount - parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 } - 1 +
        parameterRegister(index).removePrefix("p").toInt()
    return implementation!!.instructions.single {
        it.opcode in listOf(Opcode.IPUT, Opcode.IPUT_OBJECT) && (it as TwoRegisterInstruction).registerA == register
    }.getReference<FieldReference>()!!
}

private fun MutableClass.addReader(name: String, field: FieldReference) {
    val wide = field.type == "D"
    val objectType = field.type.startsWith("L")
    val read = if (wide) "iget-wide" else if (objectType) "iget-object" else "iget"
    val returnOpcode = if (wide) "return-wide" else if (objectType) "return-object" else "return"
    addMethod(name, emptyList(), field.type, AccessFlags.PUBLIC.value, if (wide) 3 else 2, """
        $read v0, p0, ${descriptor(field)}
        $returnOpcode v0
    """)
}
