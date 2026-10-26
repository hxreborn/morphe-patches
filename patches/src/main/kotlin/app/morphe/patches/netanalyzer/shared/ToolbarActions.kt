/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val TOOLBAR_ACTION_METHOD = "hxToolbarAction"
private const val LABEL_FONT_SIZE_SP = 17
private const val ACTION_PADDING_HORIZONTAL_DP = 12f
private const val ACTION_PADDING_VERTICAL_DP = 8f
private const val CLICKABLE_ENABLED = 1
private const val CLICKABLE_ON_CLICK = 3
private const val ICON_BUTTON_ENABLED = 1
private const val ICON_BUTTON_REGISTERS = 8
private const val TEMPORARY_REGISTERS = 5
private const val ACTION_METHOD_PARAMETERS = 1
private const val FUNCTION3_PARAMETERS = 3
private const val STRING_TYPE = "Ljava/lang/String;"
private const val OBJECT_TYPE = "Ljava/lang/Object;"
private const val LONG_TYPE = "J"
private const val DOUBLE_TYPE = "D"
private const val INT_TYPE = "I"
private const val BOOLEAN_TYPE = "Z"
private const val GET_CLASS = "getClass"
private const val GROUP_KEY_LOAD_DISTANCE = 2
private const val GROUP_END_LOAD_DISTANCE = 2
private const val OPEN_ACTIVITY_CLICK = "$FEATURES_CLASS->onToolbarAction"
private const val EXPORT_MENU_CLICK = "Lapp/hxreborn/extension/netanalyzer/ExportMenu;->onExportMenu"
private const val MORE_VERT_ICON = "more_vert"
private const val MORE_OPTIONS = "More Options"

private class ToolbarActionReferences(
    val textComposable: MethodReference,
    val textRegisters: Int,
    val textMask: Int,
    val colorIndex: Int,
    val fontSizeIndex: Int,
    val color: FieldReference,
    val colorLocal: FieldReference,
    val currentValue: MethodReference,
    val fontSize: MethodReference,
    val clickable: Method,
    val padding: MethodReference,
    val modifierCompanion: FieldReference,
    val composerType: String,
    val textButton: MethodReference,
)

private var toolbarActionReferences: ToolbarActionReferences? = null
private val toolbarActionMethods = HashMap<String, Method>()

internal val toolbarActionsPatch = bytecodePatch {
    dependsOn(patchesMenuPatch, vectorIconsPatch)

    execute {
        toolbarActionReferences = null
        toolbarActionMethods.clear()
        iconButton = null
    }
}

private var iconButton: MethodReference? = null

private const val LAN_SCAN_DEVICE_ROW = "LanScanDeviceRow"
private const val ROW_CHEVRON_METHOD = "hxRowChevron"
private const val ROW_CHEVRON_DESCRIPTION = "Details"
private const val ROW_CHEVRON_END_DP = 8f
private const val ROW_CHEVRON_TINT = "rowSubsubtitleColor"
private const val ROW_CHEVRON_REGISTERS = 9

context(context: BytecodePatchContext)
internal fun rowChevronMethod(classDef: MutableClass): Method = with(context) {
    toolbarActionMethods.getOrPut("${classDef.type}$ROW_CHEVRON_METHOD") {
        val references = toolbarActionReferences()
        val iconType = imageVectorType()
        val icon = Fingerprint(
            filters = listOf(
                composable(LAN_SCAN_DEVICE_ROW),
                methodCall(
                    parameters = listOf(iconType, STRING_TYPE, "L", LONG_TYPE, references.composerType, INT_TYPE, INT_TYPE),
                    returnType = "V",
                ),
            ),
        ).matchSingle()
        val iconCall = icon.instructionMatches.last()
        val chevron = icon.method.instructions.take(iconCall.index).last {
            it.opcode == Opcode.INVOKE_STATIC && it.getReference<MethodReference>()!!.let { reference ->
                reference.parameterTypes.isEmpty() && reference.returnType == iconType
            }
        }.getReference<MethodReference>()!!
        val palette = references.color.definingClass
        val tintField = classDefBy(palette).propertyFields()[ROW_CHEVRON_TINT]
            ?: throw PatchException("$palette has no $ROW_CHEVRON_TINT property")
        val companion = references.modifierCompanion
        val smali = """
            invoke-static {}, ${descriptor(chevron)}
            move-result-object v0
            const-string v1, "$ROW_CHEVRON_DESCRIPTION"
            sget-object v2, ${companion.definingClass}->${companion.name}:${companion.type}
            const/4 v3, 0x0
            const/4 v4, 0x0
            const v5, ${ROW_CHEVRON_END_DP.toRawBits()}
            const/4 v6, 0x0
            invoke-static { v2, v3, v4, v5, v6 }, ${descriptor(references.padding)}
            move-result-object v2
            sget-object v3, ${references.colorLocal.definingClass}->${references.colorLocal.name}:${references.colorLocal.type}
            invoke-virtual { p0, v3 }, ${descriptor(references.currentValue)}
            move-result-object v3
            check-cast v3, $palette
            iget-wide v3, v3, $palette->$tintField:$LONG_TYPE
            move-object v5, p0
            const/4 v6, 0x0
            const/4 v7, 0x0
            invoke-static/range { v0 .. v7 }, ${descriptor(iconCall.instruction.getReference<MethodReference>()!!)}
            return-void
        """
        classDef.addMethod(
            ROW_CHEVRON_METHOD,
            listOf(references.composerType),
            "V",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            ROW_CHEVRON_REGISTERS,
            smali,
        )
    }
}

context(context: BytecodePatchContext)
internal fun addToolbarIcon(slot: ToolbarSlot, iconName: String, pathData: String, description: String, activity: String) =
    addIconButton(slot, iconName, pathData, description, OPEN_ACTIVITY_CLICK, "$EXTENSION_PACKAGE.$activity")

context(context: BytecodePatchContext)
internal fun addExportMenu(slot: ToolbarSlot, screen: String) =
    addIconButton(slot, MORE_VERT_ICON, MaterialIcons.MORE_VERT, MORE_OPTIONS, EXPORT_MENU_CLICK, screen)

context(context: BytecodePatchContext)
private fun addIconButton(
    slot: ToolbarSlot,
    iconName: String,
    pathData: String,
    description: String,
    clickFactory: String,
    clickArgument: String,
) {
    val references = toolbarActionReferences()
    val onClickType = references.clickable.parameterTypes[CLICKABLE_ON_CLICK].toString()
    val button = iconButton ?: Fingerprint(
        filters = listOf(
            composable(WIFI_PAGER_MENU),
            methodCall(
                parameters = listOf(imageVectorType(), STRING_TYPE, BOOLEAN_TYPE, onClickType, references.composerType, INT_TYPE, INT_TYPE),
                returnType = "V",
            ),
        ),
    ).matchSingle().instructionMatches.last().instruction.getReference<MethodReference>()!!.also { iconButton = it }
    val icon = iconMethod(slot.classDef, iconName, pathData)
    val smali = """
        invoke-static {}, ${descriptor(icon)}
        move-result-object v0
        const-string v1, "$description"
        const/4 v2, $ICON_BUTTON_ENABLED
        const-class v3, $onClickType
        const-string v4, "$clickArgument"
        invoke-static { v3, v4 }, $clickFactory(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;
        move-result-object v3
        check-cast v3, $onClickType
        move-object v4, p0
        const/4 v5, 0x0
        const/4 v6, 0x0
        invoke-static/range { v0 .. v6 }, ${descriptor(button)}
        return-void
    """
    val method = slot.classDef.addMethod(
        TOOLBAR_ACTION_METHOD + toolbarActionMethods.size,
        listOf(references.composerType),
        "V",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        ICON_BUTTON_REGISTERS,
        smali,
    )
    toolbarActionMethods["${slot.classDef.type}$clickFactory$clickArgument"] = method
    slot.method.addInstructions(
        slot.index,
        "invoke-static/range { v${slot.composerRegister} .. v${slot.composerRegister} }, ${descriptor(method)}",
    )
}

internal class ToolbarSlot(
    val method: MutableMethod,
    val classDef: MutableClass,
    val index: Int,
    val composerRegister: Int,
    val composerType: String,
)

context(context: BytecodePatchContext)
internal fun emptyGroupSlot(composableName: String): ToolbarSlot =
    emptyGroupSlotOrNull(composableFingerprint(composableName).matchSingle())
        ?: throw PatchException("$composableName has no empty composer group after a null check")

internal fun emptyGroupSlotOrNull(match: Match): ToolbarSlot? {
    val method = match.method
    val instructions = method.instructions.toList()
    fun groupCall(index: Int, parameterType: String) = instructions.getOrNull(index)
        ?.takeIf { it.opcode == Opcode.INVOKE_VIRTUAL }
        ?.getReference<MethodReference>()
        ?.takeIf { it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf(parameterType) }
    fun isGetClass(index: Int) = instructions[index].let { instruction ->
        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
            instruction.getReference<MethodReference>()!!.let { it.definingClass == OBJECT_TYPE && it.name == GET_CLASS }
    }
    (match.instructionMatches.first().index until instructions.size).filter(::isGetClass).forEach { getClass ->
        (getClass + 1..getClass + GROUP_KEY_LOAD_DISTANCE).forEach { index ->
            val key = groupCall(index, INT_TYPE) ?: return@forEach
            val closes = (index + 1..index + GROUP_END_LOAD_DISTANCE).any {
                groupCall(it, BOOLEAN_TYPE)?.definingClass == key.definingClass
            }
            if (closes) {
                val keyCall = instructions[index] as FiveRegisterInstruction
                return ToolbarSlot(method, match.classDef, index + 1, keyCall.registerC, key.definingClass)
            }
        }
    }
    return null
}

context(context: BytecodePatchContext)
internal fun textButtonSlot(composableName: String): ToolbarSlot {
    val menu = Fingerprint(
        filters = listOf(composable(composableName), methodCall(reference = toolbarActionReferences().textButton)),
    ).matchSingle()
    val call = menu.instructionMatches.last()
    val reference = call.instruction.getReference<MethodReference>()!!
    val composerType = toolbarActionReferences().composerType
    val parameterTypes = reference.parameterTypes.map { it.toString() }
    val composerParameter = parameterTypes.indexOf(composerType)
    if (composerParameter < 0) throw PatchException("$reference takes no composer")
    val composerRegister = call.getInstruction<RegisterRangeInstruction>().startRegister +
        registerOffsets(parameterTypes)[composerParameter]
    return ToolbarSlot(menu.method, menu.classDef, call.index, composerRegister, composerType)
}

context(context: BytecodePatchContext)
internal fun afterTextButtonSlot(composableName: String): ToolbarSlot = textButtonSlot(composableName).let { slot ->
    ToolbarSlot(slot.method, slot.classDef, slot.index + 1, slot.composerRegister, slot.composerType)
}

context(context: BytecodePatchContext)
internal fun addToolbarAction(slot: ToolbarSlot, label: String, activity: String) {
    val actionMethod = toolbarActionMethod(slot.classDef, label, "$EXTENSION_PACKAGE.$activity")
    slot.method.addInstructions(
        slot.index,
        "invoke-static/range { v${slot.composerRegister} .. v${slot.composerRegister} }, ${descriptor(actionMethod)}",
    )
}

context(context: BytecodePatchContext)
private fun toolbarActionReferences(): ToolbarActionReferences = toolbarActionReferences ?: with(context) {
    val pingMenu = emptyGroupSlot(TOOLS_PING_MENU)
    val composerType = pingMenu.composerType
    val clickable = ClickableModifierFingerprint.matchSingle().originalMethod
    val onClickType = clickable.parameterTypes[CLICKABLE_ON_CLICK].toString()
    val afterSlot = pingMenu.method.instructions.drop(pingMenu.index)
    val textButton = afterSlot.first {
        it.opcode == Opcode.INVOKE_STATIC_RANGE && it.getReference<MethodReference>()!!.let { reference ->
            reference.parameterTypes.firstOrNull()?.toString() == onClickType &&
                reference.parameterTypes.any { type -> type.toString() == composerType }
        }
    }.getReference<MethodReference>()!!
    val labelLambda = afterSlot.first { it.opcode == Opcode.NEW_INSTANCE }.getReference<TypeReference>()!!.type
    val label = classDefBy(labelLambda).methods.single {
        it.parameterTypes.size == FUNCTION3_PARAMETERS && it.returnType == OBJECT_TYPE && it.implementation != null
    }
    val instructions = label.implementation!!.instructions.toList()
    val textIndex = instructions.indexOfLast {
        it.opcode == Opcode.INVOKE_STATIC_RANGE && it.getReference<MethodReference>()!!.let { reference ->
            reference.returnType == "V" && reference.parameterTypes.firstOrNull()?.toString() == STRING_TYPE &&
                reference.parameterTypes.any { type -> type.toString() == composerType }
        }
    }
    if (textIndex < 0) throw PatchException("$labelLambda draws no label text")
    val textCall = instructions[textIndex] as RegisterRangeInstruction
    val textComposable = instructions[textIndex].getReference<MethodReference>()!!
    val offsets = registerOffsets(textComposable.parameterTypes.map { it.toString() })
    fun parameterAt(register: Int) = offsets.indexOf(register - textCall.startRegister)
    val before = instructions.subList(0, textIndex)

    val maskRegister = textCall.startRegister + textCall.registerCount - 1
    val mask = (before.last { it is NarrowLiteralInstruction && (it as OneRegisterInstruction).registerA == maskRegister }
        as NarrowLiteralInstruction).narrowLiteral
    val colorRead = before.last { it.opcode == Opcode.IGET_WIDE }
    val fontSizeCall = before.indexOfLast {
        it.opcode == Opcode.INVOKE_STATIC && it.getReference<MethodReference>()!!.let { reference ->
            reference.returnType == LONG_TYPE && reference.parameterTypes.map(CharSequence::toString) == listOf(INT_TYPE)
        }
    }
    val fontSizeRegister = (before[fontSizeCall + 1] as OneRegisterInstruction).registerA
    val currentValueIndex = before.indexOfLast {
        it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()!!.let { reference ->
            reference.definingClass == composerType && reference.returnType == OBJECT_TYPE && reference.parameterTypes.size == 1
        }
    }
    val colorLocal = before.subList(0, currentValueIndex).last { it.opcode == Opcode.SGET_OBJECT }

    val padding = HeaderWithButtonPaddingFingerprint.matchSingle().let { header ->
        header.instructionMatches.last().instruction.getReference<MethodReference>()!!
    }
    val clickableUse = Fingerprint(
        filters = listOf(composable(HEADER_WITH_BUTTON), methodCall(reference = clickable)),
    ).matchSingle()
    val clickableReceiver = when (val call = clickableUse.instructionMatches.last().instruction) {
        is RegisterRangeInstruction -> call.startRegister
        is FiveRegisterInstruction -> call.registerC
        else -> throw PatchException("Unexpected clickable call ${call.opcode} in $HEADER_WITH_BUTTON")
    }
    val modifierCompanion = clickableUse.method.instructions.take(clickableUse.instructionMatches.last().index)
        .last { it.opcode == Opcode.SGET_OBJECT && (it as OneRegisterInstruction).registerA == clickableReceiver }
        .getReference<FieldReference>()!!

    ToolbarActionReferences(
        textComposable = textComposable,
        textRegisters = textCall.registerCount,
        textMask = mask,
        colorIndex = parameterAt((colorRead as OneRegisterInstruction).registerA),
        fontSizeIndex = parameterAt(fontSizeRegister),
        color = colorRead.getReference<FieldReference>()!!,
        colorLocal = colorLocal.getReference<FieldReference>()!!,
        currentValue = before[currentValueIndex].getReference<MethodReference>()!!,
        fontSize = before[fontSizeCall].getReference<MethodReference>()!!,
        clickable = clickable,
        padding = padding,
        modifierCompanion = modifierCompanion,
        composerType = composerType,
        textButton = textButton,
    ).also { toolbarActionReferences = it }
}

context(context: BytecodePatchContext)
private fun toolbarActionMethod(classDef: MutableClass, label: String, activity: String): Method =
    toolbarActionMethods.getOrPut("${classDef.type}$label") {
        val references = toolbarActionReferences()
        val types = references.textComposable.parameterTypes.map { it.toString() }
        val offsets = registerOffsets(types)
        val modifierType = references.clickable.returnType
        val onClickType = references.clickable.parameterTypes[CLICKABLE_ON_CLICK].toString()
        val modifierIndex = types.indexOfFirst { it == modifierType }
        val composerIndex = types.indexOf(references.composerType)
        val maskIndex = types.lastIndex
        val mask = references.textMask and (1 shl modifierIndex).inv()
        val textStart = TEMPORARY_REGISTERS
        val composerRegister = textStart + references.textRegisters

        val smali = buildString {
            appendLine("const-class v0, $onClickType")
            appendLine("const-string v1, \"$activity\"")
            appendLine("invoke-static { v0, v1 }, $FEATURES_CLASS->onToolbarAction(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;")
            appendLine("move-result-object v3")
            appendLine("check-cast v3, $onClickType")
            appendLine("sget-object v0, ${references.modifierCompanion.definingClass}->${references.modifierCompanion.name}:${references.modifierCompanion.type}")
            appendLine("const/4 v1, $CLICKABLE_ENABLED")
            appendLine("const/4 v2, 0x0")
            appendLine("const/4 v4, 0x0")
            appendLine("invoke-static/range { v0 .. v4 }, ${descriptor(references.clickable)}")
            appendLine("move-result-object v0")
            appendLine("const v1, ${ACTION_PADDING_HORIZONTAL_DP.toRawBits()}")
            appendLine("const v2, ${ACTION_PADDING_VERTICAL_DP.toRawBits()}")
            appendLine("const v3, ${ACTION_PADDING_HORIZONTAL_DP.toRawBits()}")
            appendLine("const v4, ${ACTION_PADDING_VERTICAL_DP.toRawBits()}")
            appendLine("invoke-static { v0, v1, v2, v3, v4 }, ${descriptor(references.padding)}")
            appendLine("move-result-object v${textStart + offsets[modifierIndex]}")
            appendLine("move-object/from16 v0, v$composerRegister")
            appendLine("sget-object v1, ${references.colorLocal.definingClass}->${references.colorLocal.name}:${references.colorLocal.type}")
            appendLine("invoke-virtual { v0, v1 }, ${descriptor(references.currentValue)}")
            appendLine("move-result-object v1")
            appendLine("check-cast v1, ${references.color.definingClass}")
            appendLine("iget-wide v${textStart + offsets[references.colorIndex]}, v1, ${references.color.definingClass}->${references.color.name}:${references.color.type}")
            appendLine("const/16 v0, $LABEL_FONT_SIZE_SP")
            appendLine("invoke-static { v0 }, ${descriptor(references.fontSize)}")
            appendLine("move-result-wide v${textStart + offsets[references.fontSizeIndex]}")
            types.forEachIndexed { index, type ->
                val register = textStart + offsets[index]
                when (index) {
                    0 -> appendLine("const-string v$register, \"$label\"")
                    modifierIndex, references.colorIndex, references.fontSizeIndex -> Unit
                    composerIndex -> appendLine("move-object/from16 v$register, v$composerRegister")
                    maskIndex -> appendLine("const v$register, $mask")
                    else -> appendLine(if (type.isWide()) "const-wide/16 v$register, 0x0" else "const/16 v$register, 0x0")
                }
            }
            appendLine("invoke-static/range { v$textStart .. v${textStart + references.textRegisters - 1} }, ${descriptor(references.textComposable)}")
            appendLine("return-void")
        }

        classDef.addMethod(
            TOOLBAR_ACTION_METHOD + toolbarActionMethods.size,
            listOf(references.composerType),
            "V",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            TEMPORARY_REGISTERS + references.textRegisters + ACTION_METHOD_PARAMETERS,
            smali,
        )
    }

private fun registerOffsets(types: List<String>): List<Int> {
    var offset = 0
    return types.map { type ->
        val current = offset
        offset += if (type.isWide()) 2 else 1
        current
    }
}

private fun String.isWide() = this == LONG_TYPE || this == DOUBLE_TYPE
