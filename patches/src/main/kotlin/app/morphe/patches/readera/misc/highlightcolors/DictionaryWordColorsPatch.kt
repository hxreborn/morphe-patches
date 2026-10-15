/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.highlightcolors

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val DICTIONARY_WORD_COLOR_KEY = "pref_dict_word_color"

@Suppress("unused")
val dictionaryWordColorsPatch = bytecodePatch(
    name = "Dictionary word colors",
    description = "Adds a color picker for dictionary words.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val prefs = highlightColorPrefs()

        val defaultColorField: FieldReference
        val addColoredWord: MethodReference
        DictionaryWordAddFingerprint.instructionMatches.let {
            defaultColorField = it[2].instruction.getReference<FieldReference>()!!
            addColoredWord = it[3].instruction.getReference<MethodReference>()!!
        }
        readDefaultColorFromPreference(defaultColorField, DICTIONARY_WORD_COLOR_KEY)

        val selectionType = addColoredWord.parameterTypes[1].toString()
        val wordType = addColoredWord.returnType
        val saveWordColor = DictionaryWordColorFingerprint.method
        val wordColorField = DictionaryWordColorFingerprint.instructionMatches[0].instruction
            .getReference<FieldReference>()!!
        val postWordEvent = DictionaryWordEventFingerprint.method
        val wordEventType = postWordEvent.parameterTypes[2].toString()
        val eventBusAccessor = CreateQuoteFingerprint.instructionMatches[1].instruction.getReference<MethodReference>()!!
        val showWordHint = dictionaryWordHintFingerprint(DictionaryWordAddFingerprint.method)
            .instructionMatches[1].instruction.getReference<MethodReference>()!!

        val unavailableCheck: MethodReference
        val unavailableMessageId: Long
        val showMessage: MethodReference
        DictionaryUnavailableFingerprint.let {
            unavailableCheck = it.instructionMatches[0].instruction.getReference<MethodReference>()!!
            unavailableMessageId = it.method.getInstruction<WideLiteralInstruction>(it.instructionMatches[4].index).wideLiteral
            showMessage = it.instructionMatches[5].instruction.getReference<MethodReference>()!!
        }
        val unavailableEvent: String
        val findStickyEvent: MethodReference
        dictionaryUnavailableCheckFingerprint(unavailableCheck).instructionMatches.let {
            unavailableEvent = it[0].instruction.getReference<TypeReference>()!!.type
            findStickyEvent = it[1].instruction.getReference<MethodReference>()!!
        }

        val selectedWordType = selectionStateClass().fields.single { field ->
            classDefByOrNull(field.type)?.fields?.any { it.type == wordType } == true
        }.type
        val selectedWordModel = classDefBy(selectedWordType).fields.single { it.type == wordType }

        fillHighlightColorStub(
            stubMatchIndex = DICTIONARY_PICKER_MATCH,
            pickerFactory = "forDictionary",
            targetType = selectedWordType,
            selectionType = selectionType,
            prefs = prefs,
            defaultColorField = defaultColorField,
            readTargetColor = """
                iget-object v0, v0, $selectedWordModel
                iget v0, v0, $wordColorField
            """,
        ).let { stub ->
            stub.addColorSetter(
                prefs,
                DICTIONARY_WORD_COLOR_KEY,
                registers = 6,
                """
                    iget-object v0, p0, ${stub.targetField}
                    if-eqz v0, :create
                    iget-object v0, v0, $selectedWordModel
                    invoke-static { v0, p1 }, $saveWordColor
                    iget-object v1, p0, ${stub.activityField}
                    invoke-virtual { v1 }, $eventBusAccessor
                    move-result-object v1
                    const-string v2, "COLORED"
                    invoke-static { v2 }, $wordEventType->valueOf(Ljava/lang/String;)$wordEventType
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, $postWordEvent
                    return-void
                    :create
                    iget-object v0, p0, ${stub.selectionField}
                    if-eqz v0, :done
                    iget-object v1, p0, ${stub.activityField}
                    invoke-virtual { v1 }, $eventBusAccessor
                    move-result-object v2
                    const-class v3, $unavailableEvent
                    invoke-virtual { v2, v3 }, $findStickyEvent
                    move-result-object v2
                    if-eqz v2, :add
                    const v2, $unavailableMessageId
                    invoke-static { v1, v2 }, $showMessage
                    return-void
                    :add
                    invoke-static { v1, v0, p1 }, $addColoredWord
                    invoke-virtual { v1 }, $showWordHint
                    :done
                    return-void
                """,
            )
        }
    }
}
