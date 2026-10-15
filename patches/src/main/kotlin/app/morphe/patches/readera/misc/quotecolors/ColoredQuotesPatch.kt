/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.quotecolors

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val PICKER_CLASS = "Lapp/hxreborn/extension/readera/QuoteColorPicker;"
private const val HOST_INTERFACE = "Lapp/hxreborn/extension/readera/QuoteColorHost;"
private const val READ_ACTIVITY_CLASS = "Lorg/readera/read/ReadActivity;"
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
        val prefsAccessor: MethodReference
        val defaultColorField: FieldReference
        val paletteFilter: MethodReference
        QuoteColorIconFingerprint.instructionMatches.let {
            prefsAccessor = it[0].instruction.getReference<MethodReference>()!!
            defaultColorField = it[2].instruction.getReference<FieldReference>()!!
            paletteFilter = it[3].instruction.getReference<MethodReference>()!!
        }
        val prefsClass = defaultColorField.definingClass
        val paletteClass = paletteFilter.definingClass
        val prefsStore = prefsStoreFingerprint(prefsClass).method
        val prefsReload = prefsReloadFingerprint(prefsClass).method
        val paletteByIndex = paletteByIndexFingerprint(paletteClass).method

        defaultQuoteColorWriteFingerprint(defaultColorField).let {
            val storeField = it.instructionMatches[0].instruction.getReference<FieldReference>()!!
            val writeIndex = it.instructionMatches[1].index
            val colorRegister = it.method.getFreeRegisterProvider(writeIndex, 1).getFreeRegister4Bit()
            it.method.replaceInstruction(writeIndex, "iput v$colorRegister, p0, $defaultColorField")
            it.method.addInstructions(
                writeIndex,
                """
                    sget-object v$colorRegister, $storeField
                    invoke-static { v$colorRegister }, $PICKER_CLASS->readDefaultColor(Landroid/content/SharedPreferences;)I
                    move-result v$colorRegister
                """,
            )
        }

        val (dayColorField, nightColorField) = paletteConstructorFingerprint(paletteClass).method.let { constructor ->
            val firstParameterRegister = constructor.implementation!!.registerCount - constructor.parameters.size
            val fieldByParameter = constructor.implementation!!.instructions
                .filter { it.opcode == Opcode.IPUT }
                .associate {
                    (it as TwoRegisterInstruction).registerA - firstParameterRegister to it.getReference<FieldReference>()!!
                }
            fieldByParameter.getValue(PALETTE_DAY_PARAMETER) to fieldByParameter.getValue(PALETTE_NIGHT_PARAMETER)
        }

        val nightCheck = NightHighlightFingerprint.method
        val colorModeType = NightHighlightFingerprint.instructionMatches[0].instruction
            .getReference<MethodReference>()!!.returnType
        val nightField = NightHighlightFingerprint.instructionMatches[2].instruction.getReference<FieldReference>()!!

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
                            invoke-direct { p0 }, $nightCheck
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

        SelectionActionsFingerprint.let { match ->
            val containerId = match.method.getInstruction<WideLiteralInstruction>(match.instructionMatches[2].index).wideLiteral
            val dictionaryGroupId = match.method.getInstruction<WideLiteralInstruction>(match.instructionMatches[4].index).wideLiteral
            val findView = match.instructionMatches[3].instruction.getReference<MethodReference>()!!
            val pickerStub = mutableClassDefBy(
                match.instructionMatches[9].instruction.getReference<TypeReference>()!!.type,
            )
            val stubBase = classDefBy(pickerStub.superclass!!)
            fun stubMethod(parameters: List<String>) = stubBase.methods.single { method ->
                method.returnType == "V" && method.name != "<init>" &&
                    method.parameterTypes.map { it.toString() } == parameters
            }
            val hideMethod = stubMethod(listOf("Z"))
            val showMethod = stubMethod(listOf())
            val colorModeMethod = stubMethod(listOf(colorModeType))
            val bindMethod = stubBase.methods.single { method ->
                method.returnType == "V" && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].toString() !in listOf("Z", colorModeType)
            }
            val selectionStateType = bindMethod.parameterTypes[0].toString()
            val selectionStateClass = classDefBy(selectionStateType)
            val stateSelectionField = selectionStateClass.fields.single { it.type == selectionType }
            val stateQuoteField = selectionStateClass.fields.single { it.type == quoteType }

            val pickerField = pickerStub.addField("picker", PICKER_CLASS)
            val activityField = pickerStub.addField("activity", READ_ACTIVITY_CLASS)
            val quoteField = pickerStub.addField("quote", quoteType)
            val selectionField = pickerStub.addField("selection", selectionType)

            pickerStub.interfaces.add(HOST_INTERFACE)
            val constructor = pickerStub.methods.single { it.name == "<init>" }
            pickerStub.methods.remove(constructor)
            pickerStub.methods.add(
                constructor.copy(registers = 6).apply {
                    addInstructions(
                        0,
                        """
                            invoke-direct { p0 }, ${pickerStub.superclass}-><init>()V
                            iput-object p1, p0, $activityField
                            const v0, $containerId
                            invoke-interface { p2, v0 }, $findView
                            move-result-object v0
                            check-cast v0, Landroid/view/ViewGroup;
                            const v1, $dictionaryGroupId
                            invoke-interface { p2, v1 }, $findView
                            move-result-object v1
                            check-cast v1, Landroid/view/ViewGroup;
                            new-instance v2, $PICKER_CLASS
                            invoke-direct { v2, p1, v0, v1, p0 }, $PICKER_CLASS-><init>(Landroid/app/Activity;Landroid/view/ViewGroup;Landroid/view/ViewGroup;$HOST_INTERFACE)V
                            iput-object v2, p0, $pickerField
                            return-void
                        """,
                    )
                },
            )

            pickerStub.addOverride(hideMethod, registers = 3) {
                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $pickerField
                        invoke-virtual { v0, p1 }, $PICKER_CLASS->hide(Z)V
                        if-eqz p1, :keep
                        const/4 v0, 0x0
                        iput-object v0, p0, $quoteField
                        iput-object v0, p0, $selectionField
                        :keep
                        return-void
                    """,
                )
            }
            pickerStub.addOverride(showMethod, registers = 2) {
                addInstructions(
                    0,
                    """
                        iget-object v0, p0, $pickerField
                        invoke-virtual { v0 }, $PICKER_CLASS->show()V
                        return-void
                    """,
                )
            }
            pickerStub.addOverride(colorModeMethod, registers = 4) {
                addInstructions(
                    0,
                    """
                        iget-boolean v0, p1, $nightField
                        iget-object v1, p0, $pickerField
                        invoke-virtual { v1, v0 }, $PICKER_CLASS->setNight(Z)V
                        return-void
                    """,
                )
            }
            pickerStub.addOverride(bindMethod, registers = 3) {
                addInstructions(
                    0,
                    """
                        iget-object v0, p1, $stateSelectionField
                        iput-object v0, p0, $selectionField
                        iget-object v0, p1, $stateQuoteField
                        iput-object v0, p0, $quoteField
                        iget-object v0, p0, $pickerField
                        invoke-virtual { v0 }, $PICKER_CLASS->refresh()V
                        return-void
                    """,
                )
            }
            pickerStub.addPublicMethod("getQuoteColor", listOf(), "I", registers = 2) {
                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $quoteField
                        if-eqz v0, :fallback
                        iget v0, v0, $quoteColorField
                        return v0
                        :fallback
                        invoke-static { }, $prefsAccessor
                        move-result-object v0
                        iget v0, v0, $defaultColorField
                        return v0
                    """,
                )
            }
            pickerStub.addPublicMethod("getQuoteColorFilter", listOf("I"), "Landroid/graphics/ColorFilter;", registers = 2) {
                addInstructions(
                    0,
                    """
                        invoke-static { p1 }, $paletteFilter
                        move-result-object v0
                        return-object v0
                    """,
                )
            }
            pickerStub.addPublicMethod("setQuoteColor", listOf("I"), "V", registers = 5) {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static { }, $prefsStore
                        move-result-object v0
                        invoke-static { v0, p1 }, $PICKER_CLASS->saveDefaultColor(Landroid/content/SharedPreferences;I)V
                        invoke-static { }, $prefsReload
                        iget-object v0, p0, $quoteField
                        if-eqz v0, :create
                        iput p1, v0, $quoteColorField
                        iget-object v1, p0, $activityField
                        invoke-virtual { v1 }, $eventBusAccessor
                        move-result-object v1
                        const-string v2, "COLORED"
                        invoke-static { v2 }, $eventType->valueOf(Ljava/lang/String;)$eventType
                        move-result-object v2
                        invoke-static { v1, v0, v2 }, $postQuoteEvent
                        return-void
                        :create
                        iget-object v0, p0, $selectionField
                        if-eqz v0, :done
                        iget-object v1, p0, $activityField
                        const/4 v2, 0x0
                        invoke-static { v1, v0, p1, v2 }, $createQuote
                        :done
                        return-void
                    """,
                )
            }
        }
    }
}

private fun Method.copy(registers: Int) = ImmutableMethod(
    definingClass,
    name,
    parameters,
    returnType,
    accessFlags,
    annotations,
    hiddenApiRestrictions,
    MutableMethodImplementation(registers),
).toMutable()

private fun MutableClass.addField(name: String, type: String): String {
    fields.add(
        ImmutableField(this.type, name, type, AccessFlags.PRIVATE.value, null, null, null).toMutable(),
    )
    return "${this.type}->$name:$type"
}

private fun MutableClass.addOverride(base: Method, registers: Int, body: MutableMethod.() -> Unit) =
    addPublicMethod(base.name, base.parameterTypes.map { it.toString() }, base.returnType, registers, body)

private fun MutableClass.addPublicMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    registers: Int,
    body: MutableMethod.() -> Unit,
) {
    methods.add(
        ImmutableMethod(
            type,
            name,
            parameters.map { ImmutableMethodParameter(it, null, null) },
            returnType,
            AccessFlags.PUBLIC.value,
            null,
            null,
            MutableMethodImplementation(registers),
        ).toMutable().apply(body),
    )
}
