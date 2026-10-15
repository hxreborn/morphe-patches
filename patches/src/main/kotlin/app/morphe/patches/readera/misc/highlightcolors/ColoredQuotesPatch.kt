/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.highlightcolors

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CITATION_COLOR_KEY = "pref_citation_color"
private const val PALETTE_DAY_PARAMETER = 4
private const val PALETTE_NIGHT_PARAMETER = 5

@Suppress("unused")
val coloredQuotesPatch = bytecodePatch(
    name = "Colored quotes",
    description = "Adds a color picker for quotes and highlights each quote in its own color.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val prefs = highlightColorPrefs()
        val defaultColorField = QuoteColorIconFingerprint.instructionMatches[2].instruction.getReference<FieldReference>()!!
        val paletteClass = prefs.paletteFilter.definingClass
        val paletteByIndex = paletteByIndexFingerprint(paletteClass).method

        readDefaultColorFromPreference(defaultColorField, CITATION_COLOR_KEY)

        val (dayColorField, nightColorField) = paletteConstructorFingerprint(paletteClass).method.let { constructor ->
            val firstParameterRegister = constructor.implementation!!.registerCount - constructor.parameters.size
            val fieldByParameter = constructor.implementation!!.instructions
                .filter { it.opcode == Opcode.IPUT }
                .associate {
                    (it as TwoRegisterInstruction).registerA - firstParameterRegister to it.getReference<FieldReference>()!!
                }
            fieldByParameter.getValue(PALETTE_DAY_PARAMETER) to fieldByParameter.getValue(PALETTE_NIGHT_PARAMETER)
        }

        val eventBusAccessor: MethodReference
        val eventType: String
        val postQuoteEvent: MethodReference
        CreateQuoteFingerprint.instructionMatches.let {
            eventBusAccessor = it[1].instruction.getReference<MethodReference>()!!
            eventType = it[3].instruction.getReference<FieldReference>()!!.type
            postQuoteEvent = it[4].instruction.getReference<MethodReference>()!!
        }
        val createQuote = CreateQuoteFingerprint.method
        val selectionType = createQuote.parameterTypes[1].toString()
        val quoteType = createQuote.returnType
        val quoteColorField = classDefBy(quoteType).fields.single { field ->
            field.type == "I" && field.accessFlags and AccessFlags.STATIC.value == 0
        }

        QuoteHighlightColorFingerprint.let {
            val highlight = it.method
            it.classDef.methods.remove(highlight)
            it.classDef.methods.add(
                highlight.copy(registers = 4).apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            instance-of v0, p1, $quoteType
                            if-eqz v0, :fallback
                            check-cast p1, $quoteType
                            iget v0, p1, $quoteColorField
                            invoke-static { v0 }, $paletteByIndex
                            move-result-object v0
                            invoke-direct { p0 }, ${NightHighlightFingerprint.method}
                            move-result v1
                            if-eqz v1, :day
                            iget v0, v0, $nightColorField
                            return v0
                            :day
                            iget v0, v0, $dayColorField
                            return v0
                            :fallback
                            const v0, $DEFAULT_QUOTE_HIGHLIGHT
                            return v0
                        """,
                    )
                },
            )
        }

        fillHighlightColorStub(
            stubMatchIndex = QUOTE_PICKER_MATCH,
            pickerFactory = "forQuotes",
            targetType = quoteType,
            selectionType = selectionType,
            prefs = prefs,
            defaultColorField = defaultColorField,
            readTargetColor = "iget v0, v0, $quoteColorField",
        ).let { stub ->
            stub.addColorSetter(
                prefs,
                CITATION_COLOR_KEY,
                registers = 5,
                """
                    iget-object v0, p0, ${stub.targetField}
                    if-eqz v0, :create
                    iput p1, v0, $quoteColorField
                    iget-object v1, p0, ${stub.activityField}
                    invoke-virtual { v1 }, $eventBusAccessor
                    move-result-object v1
                    const-string v2, "COLORED"
                    invoke-static { v2 }, $eventType->valueOf(Ljava/lang/String;)$eventType
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, $postQuoteEvent
                    return-void
                    :create
                    iget-object v0, p0, ${stub.selectionField}
                    if-eqz v0, :done
                    iget-object v1, p0, ${stub.activityField}
                    const/4 v2, 0x0
                    invoke-static { v1, v0, p1, v2 }, $createQuote
                    :done
                    return-void
                """,
            )
        }
    }
}
