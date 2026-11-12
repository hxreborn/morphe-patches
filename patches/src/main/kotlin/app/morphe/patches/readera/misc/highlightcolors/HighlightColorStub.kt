/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.highlightcolors

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.smali.addMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal const val PICKER_CLASS = "Lapp/hxreborn/extension/readera/HighlightColorPicker;"
internal const val READ_ACTIVITY_CLASS = "Lorg/readera/read/ReadActivity;"
private const val HOST_INTERFACE = "Lapp/hxreborn/extension/readera/HighlightColorHost;"
private const val PICKER_FACTORY_SIGNATURE =
    "(Landroid/app/Activity;Landroid/view/ViewGroup;Landroid/view/ViewGroup;$HOST_INTERFACE)$PICKER_CLASS"

internal class HighlightColorPrefs(
    val accessor: MethodReference,
    val store: MethodReference,
    val reload: MethodReference,
    val paletteFilter: MethodReference,
)

internal class HighlightColorStub(
    val type: MutableClass,
    val activityField: String,
    val targetField: String,
    val selectionField: String,
)

internal fun BytecodePatchContext.highlightColorPrefs(): HighlightColorPrefs {
    val prefsAccessor = QuoteColorIconFingerprint.instructionMatches[0].instruction.getReference<MethodReference>()!!
    val paletteFilter = QuoteColorIconFingerprint.instructionMatches[3].instruction.getReference<MethodReference>()!!
    val prefsClass = prefsAccessor.definingClass
    return HighlightColorPrefs(
        prefsAccessor,
        prefsStoreFingerprint(prefsClass).method,
        prefsReloadFingerprint(prefsClass).method,
        paletteFilter,
    )
}

internal fun BytecodePatchContext.readDefaultColorFromPreference(defaultColorField: FieldReference, key: String) {
    defaultColorWriteFingerprint(defaultColorField).let {
        val storeField = it.instructionMatches[0].instruction.getReference<FieldReference>()!!
        val writeIndex = it.instructionMatches[1].index
        val registers = it.method.getFreeRegisterProvider(writeIndex, 2)
        val colorRegister = registers.getFreeRegister4Bit()
        val keyRegister = registers.getFreeRegister4Bit()
        it.method.replaceInstruction(writeIndex, "iput v$colorRegister, p0, $defaultColorField")
        it.method.addInstructions(
            writeIndex,
            """
                sget-object v$colorRegister, $storeField
                const-string v$keyRegister, "$key"
                invoke-static { v$colorRegister, v$keyRegister }, $PICKER_CLASS->readDefaultColor(Landroid/content/SharedPreferences;Ljava/lang/String;)I
                move-result v$colorRegister
            """,
        )
    }
}

internal fun BytecodePatchContext.selectionStateClass(): ClassDef {
    val stubType = SelectionActionsFingerprint.instructionMatches[QUOTE_PICKER_MATCH].instruction
        .getReference<TypeReference>()!!.type
    val colorModeType = NightHighlightFingerprint.instructionMatches[0].instruction
        .getReference<MethodReference>()!!.returnType
    val bindMethod = stubBindMethod(classDefBy(classDefBy(stubType).superclass!!), colorModeType)
    return classDefBy(bindMethod.parameterTypes[0].toString())
}

private fun stubBindMethod(stubBase: ClassDef, colorModeType: String) = stubBase.methods.single { method ->
    method.returnType == "V" && method.parameterTypes.size == 1 &&
        method.parameterTypes[0].toString() !in listOf("Z", colorModeType)
}

internal fun BytecodePatchContext.fillHighlightColorStub(
    stubMatchIndex: Int,
    pickerFactory: String,
    targetType: String,
    selectionType: String,
    prefs: HighlightColorPrefs,
    defaultColorField: FieldReference,
    readTargetColor: String,
): HighlightColorStub {
    val selectionActions = SelectionActionsFingerprint
    val quoteGroupId = selectionActions.method
        .getInstruction<WideLiteralInstruction>(selectionActions.instructionMatches[QUOTE_GROUP_ID_MATCH].index).wideLiteral
    val dictionaryGroupId = selectionActions.method
        .getInstruction<WideLiteralInstruction>(selectionActions.instructionMatches[DICTIONARY_GROUP_ID_MATCH].index).wideLiteral
    val findView = selectionActions.instructionMatches[FIND_VIEW_MATCH].instruction.getReference<MethodReference>()!!
    val stub = mutableClassDefBy(
        selectionActions.instructionMatches[stubMatchIndex].instruction.getReference<TypeReference>()!!.type,
    )

    val colorModeType = NightHighlightFingerprint.instructionMatches[0].instruction
        .getReference<MethodReference>()!!.returnType
    val nightField = NightHighlightFingerprint.instructionMatches[2].instruction.getReference<FieldReference>()!!

    val stubBase = classDefBy(stub.superclass!!)
    fun stubMethod(parameters: List<String>) = stubBase.methods.single { method ->
        method.returnType == "V" && method.name != "<init>" &&
            method.parameterTypes.map { it.toString() } == parameters
    }
    val hideMethod = stubMethod(listOf("Z"))
    val showMethod = stubMethod(listOf())
    val colorModeMethod = stubMethod(listOf(colorModeType))
    val bindMethod = stubBindMethod(stubBase, colorModeType)
    val selectionStateClass = classDefBy(bindMethod.parameterTypes[0].toString())
    val stateSelectionField = selectionStateClass.fields.single { it.type == selectionType }
    val stateTargetField = selectionStateClass.fields.single { it.type == targetType }

    val pickerField = stub.addField("picker", PICKER_CLASS)
    val activityField = stub.addField("activity", READ_ACTIVITY_CLASS)
    val targetField = stub.addField("target", targetType)
    val selectionField = stub.addField("selection", selectionType)

    stub.interfaces.add(HOST_INTERFACE)
    val constructor = stub.methods.single { it.name == "<init>" }
    stub.methods.remove(constructor)
    stub.methods.add(
        constructor.copy(registers = 6).apply {
            addInstructions(
                0,
                """
                    invoke-direct { p0 }, ${stub.superclass}-><init>()V
                    iput-object p1, p0, $activityField
                    const v0, $quoteGroupId
                    invoke-interface { p2, v0 }, $findView
                    move-result-object v0
                    check-cast v0, Landroid/view/ViewGroup;
                    const v1, $dictionaryGroupId
                    invoke-interface { p2, v1 }, $findView
                    move-result-object v1
                    check-cast v1, Landroid/view/ViewGroup;
                    invoke-static { p1, v0, v1, p0 }, $PICKER_CLASS->$pickerFactory$PICKER_FACTORY_SIGNATURE
                    move-result-object v2
                    iput-object v2, p0, $pickerField
                    return-void
                """,
            )
        },
    )

    stub.addOverride(
        hideMethod,
        registers = 3,
        """
            iget-object v0, p0, $pickerField
            invoke-virtual { v0, p1 }, $PICKER_CLASS->hide(Z)V
            if-eqz p1, :keep
            const/4 v0, 0x0
            iput-object v0, p0, $targetField
            iput-object v0, p0, $selectionField
            :keep
            return-void
        """,
    )
    stub.addOverride(
        showMethod,
        registers = 2,
        """
            iget-object v0, p0, $pickerField
            invoke-virtual { v0 }, $PICKER_CLASS->show()V
            return-void
        """,
    )
    stub.addOverride(
        colorModeMethod,
        registers = 4,
        """
            iget-boolean v0, p1, $nightField
            iget-object v1, p0, $pickerField
            invoke-virtual { v1, v0 }, $PICKER_CLASS->setNight(Z)V
            return-void
        """,
    )
    stub.addOverride(
        bindMethod,
        registers = 3,
        """
            iget-object v0, p1, $stateSelectionField
            iput-object v0, p0, $selectionField
            iget-object v0, p1, $stateTargetField
            iput-object v0, p0, $targetField
            iget-object v0, p0, $pickerField
            invoke-virtual { v0 }, $PICKER_CLASS->refresh()V
            return-void
        """,
    )
    stub.addMethod(
        "getHighlightColor",
        listOf(),
        "I",
        AccessFlags.PUBLIC.value,
        registers = 2,
        """
            iget-object v0, p0, $targetField
            if-eqz v0, :fallback
            $readTargetColor
            return v0
            :fallback
            invoke-static { }, ${prefs.accessor}
            move-result-object v0
            iget v0, v0, $defaultColorField
            return v0
        """,
    )
    stub.addMethod(
        "getHighlightColorFilter",
        listOf("I"),
        "Landroid/graphics/ColorFilter;",
        AccessFlags.PUBLIC.value,
        registers = 2,
        """
            invoke-static { p1 }, ${prefs.paletteFilter}
            move-result-object v0
            return-object v0
        """,
    )

    return HighlightColorStub(stub, activityField, targetField, selectionField)
}

internal fun HighlightColorStub.addColorSetter(prefs: HighlightColorPrefs, key: String, registers: Int, body: String) =
    type.addMethod(
        "setHighlightColor",
        listOf("I"),
        "V",
        AccessFlags.PUBLIC.value,
        registers,
        """
            invoke-static { }, ${prefs.store}
            move-result-object v0
            const-string v1, "$key"
            invoke-static { v0, v1, p1 }, $PICKER_CLASS->saveDefaultColor(Landroid/content/SharedPreferences;Ljava/lang/String;I)V
            invoke-static { }, ${prefs.reload}
        """ + body,
    )

internal fun Method.copy(registers: Int) = ImmutableMethod(
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

private fun MutableClass.addOverride(base: Method, registers: Int, smali: String) =
    addMethod(base.name, base.parameterTypes.map { it.toString() }, base.returnType, AccessFlags.PUBLIC.value, registers, smali)
