/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SWITCH_THUMB_ICON_CLASS = "Lapp/hxreborn/extension/protonvpn/SwitchThumbIcon;"
private const val VIEW_SWITCH_STYLE_CLASS = "Lapp/hxreborn/extension/protonvpn/ViewSwitchStyle;"
private const val THUMB_CONTENT_PARAMETER = 3
private const val SWITCH_COLORS_PARAMETER = 5
private const val THUMB_CONTENT_DEFAULT_BIT = 0x8
private const val CHECKED_ICON_COLOR_PARAMETER = 3
private const val CHECKED_ICON_COLOR_DEFAULT_BIT = 0x8
private const val UNCHECKED_TRACK_COLOR_PARAMETER = 5
private const val UNCHECKED_TRACK_COLOR_DEFAULT_BIT = 0x20
private const val UNCHECKED_ICON_COLOR_PARAMETER = 7
private const val UNCHECKED_ICON_COLOR_DEFAULT_BIT = 0x80
private const val CHECKED_TRACK_COLOR_ARGUMENT = 1
private const val UNCHECKED_TRACK_COLOR_ARGUMENT = 5
private const val SWITCH_ICON_SIZE_DP = 16f

private fun BytecodePatchContext.addDrawIconMethod() {
    val checkmarkId = resourceId(ResourceType.DRAWABLE, "ic_proton_checkmark")
    val crossId = resourceId(ResourceType.DRAWABLE, "ic_proton_cross")

    val callers = IconCallerFingerprint.matchAll()
    val (painterResource, icon) = callers.firstNotNullOfOrNull { caller ->
        val painter = caller.instructionMatches[1].instruction.getReference<MethodReference>()!!
        val icon = caller.instructionMatches[2].instruction.getReference<MethodReference>()!!
        (painter to icon).takeIf { isPainterResource(painter, icon) }
    } ?: throw PatchException("No painterResource call feeding Icon for $checkmarkId")
    val composerType = icon.parameterTypes[4].toString()
    val modifierType = icon.parameterTypes[2].toString()

    val defaultIconSize = defaultIconSizeModifierFingerprint(icon.definingClass).matchSingle()
    val companion = defaultIconSize.instructionMatches.first().instruction.getReference<FieldReference>()!!
    val size = defaultIconSize.instructionMatches.last().instruction.getReference<MethodReference>()!!
    val companionIsModifier = companion.type == modifierType || modifierType in classDefBy(companion.type).interfaces
    if (!companionIsModifier || size.parameterTypes.first().toString() != modifierType) {
        throw PatchException("Default icon size does not read $modifierType: ${descriptor(companion)}, ${descriptor(size)}")
    }

    var nextInt = 0
    val painterArguments = painterResource.parameterTypes.joinToString { type ->
        when {
            type.toString() == composerType -> "p0"
            nextInt++ == 0 -> "v0"
            else -> "v1"
        }
    }

    mutableClassDefBy(SWITCH_THUMB_ICON_CLASS).replaceStub(
        "drawIcon",
        13,
        """
            if-eqz p1, :unchecked
            const v0, $checkmarkId
            goto :draw
            :unchecked
            const v0, $crossId
            :draw
            check-cast p0, $composerType
            const/4 v1, 0x0
            invoke-static { $painterArguments }, ${descriptor(painterResource)}
            move-result-object v0
            const/4 v1, 0x0
            sget-object v2, ${descriptor(companion)}
            const v3, ${SWITCH_ICON_SIZE_DP.toRawBits()}
            invoke-static { v2, v3 }, ${descriptor(size)}
            move-result-object v2
            move-wide v3, p2
            move-object v5, p0
            const/4 v6, 0x0
            move v7, p4
            invoke-static/range { v0 .. v7 }, ${descriptor(icon)}
            return-void
        """,
    )
}

private fun isPainterResource(painter: MethodReference, icon: MethodReference): Boolean {
    val types = painter.parameterTypes.map(CharSequence::toString)
    return painter.returnType == icon.parameterTypes[0].toString() &&
        types.count { it == "I" } == 2 && types.count { it == icon.parameterTypes[4].toString() } == 1
}

private class ColorTokenCalls(
    val tokensInstance: FieldReference,
    val tokenGetter: MethodReference,
    val tokenColor: MethodReference,
)

private fun BytecodePatchContext.checkedTrackColorCalls(switchColors: MethodReference): ColorTokenCalls {
    val match = checkedTrackColorDefaultFingerprint(switchColors).matchSingle()
    val (_, tokensInstance, tokenGetter, tokenColor) = match.instructionMatches.map { match.method.getInstruction(it.index) }
    return ColorTokenCalls(
        tokensInstance.getReference<FieldReference>()!!,
        tokenGetter.getReference<MethodReference>()!!,
        tokenColor.getReference<MethodReference>()!!,
    )
}

private fun BytecodePatchContext.injectCheckedIconColor() {
    val switchColorsType = ProtonSwitchFingerprint.matchSingle().method.parameterTypes[SWITCH_COLORS_PARAMETER].toString()
    val match = protonSwitchColorsFingerprint(switchColorsType).matchSingle()
    val callIndex = match.instructionMatches.last().index
    val call = match.method.getInstruction<RegisterRangeInstruction>(callIndex)
    val colors = call.getReference<MethodReference>()!!
    val trackColor = checkedTrackColorCalls(colors)
    val composerType = trackColor.tokenColor.parameterTypes[1]

    val checkedIconColor = mutableClassDefBy(SWITCH_THUMB_ICON_CLASS).addStaticMethod(
        "checkedIconColor",
        listOf(composerType.toString()),
        "J",
        3,
        """
            sget-object v0, ${descriptor(trackColor.tokensInstance)}
            invoke-virtual { v0 }, ${descriptor(trackColor.tokenGetter)}
            move-result-object v0
            const/4 v1, 0x6
            invoke-static { v0, p0, v1 }, ${descriptor(trackColor.tokenColor)}
            move-result-wide v0
            return-wide v0
        """,
    )
    val parameterRegisters = colors.parameterTypes.runningFold(call.startRegister + 1) { register, type ->
        register + if (type == "J" || type == "D") 2 else 1
    }
    val composer = parameterRegisters[colors.parameterTypes.indexOf(composerType)]
    val defaultMask = parameterRegisters[colors.parameterTypes.lastIndex]
    val defaults = match.method.literalWrittenTo(defaultMask, callIndex).toInt()
    if (defaults and UNCHECKED_TRACK_COLOR_DEFAULT_BIT != 0) {
        throw PatchException("Proton switch colors leave the unchecked track color to the Material default")
    }
    val iconColorDefaults = CHECKED_ICON_COLOR_DEFAULT_BIT or UNCHECKED_ICON_COLOR_DEFAULT_BIT

    match.method.addInstructions(
        callIndex,
        """
            invoke-static/range { v$composer .. v$composer }, $checkedIconColor
            move-result-wide v${parameterRegisters[CHECKED_ICON_COLOR_PARAMETER]}
            move-wide/from16 v${parameterRegisters[UNCHECKED_ICON_COLOR_PARAMETER]}, v${parameterRegisters[UNCHECKED_TRACK_COLOR_PARAMETER]}
            const v$defaultMask, ${defaults and iconColorDefaults.inv()}
        """,
    )
}

private fun BytecodePatchContext.injectCheckedThumbColor() {
    val colorsType = ProtonSwitchFingerprint.matchOrNull()?.method?.parameterTypes?.get(SWITCH_COLORS_PARAMETER)
        ?: switchImplFingerprint().matchSingle().method.parameterTypes.first { isSwitchColorsType(it) }
    val constructor = mutableClassDefBy(colorsType.toString()).methods.single { it.isSwitchColorsConstructor() }

    constructor.addInstructions(
        0,
        """
            invoke-static/range { p1 .. p2 }, $SWITCH_THUMB_ICON_CLASS->checkedThumbColor(J)J
            move-result-wide p1
        """,
    )
}

private fun BytecodePatchContext.injectThumbContent() {
    val protonSwitch = ProtonSwitchFingerprint.matchSingle().method
    val thumbContentType = protonSwitch.parameterTypes[THUMB_CONTENT_PARAMETER].toString()
    val defaultMask = "p${protonSwitch.parameterTypes.lastIndex}"
    val registers = protonSwitch.getFreeRegisterProvider(0, 2)
    val contentRegister = registers.getFreeRegister4Bit()
    val checkedRegister = registers.getFreeRegister4Bit()

    protonSwitch.addInstructionsWithLabels(
        0,
        """
            and-int/lit8 v$contentRegister, $defaultMask, $THUMB_CONTENT_DEFAULT_BIT
            if-nez v$contentRegister, :inject
            if-nez p$THUMB_CONTENT_PARAMETER, :original
            :inject
            const-class v$contentRegister, $thumbContentType
            move/from16 v$checkedRegister, p0
            invoke-static { v$contentRegister, v$checkedRegister }, $SWITCH_THUMB_ICON_CLASS->thumbContent(Ljava/lang/Class;Z)Ljava/lang/Object;
            move-result-object v$contentRegister
            check-cast v$contentRegister, $thumbContentType
            move-object/from16 p$THUMB_CONTENT_PARAMETER, v$contentRegister
            and-int/lit8 $defaultMask, $defaultMask, ${THUMB_CONTENT_DEFAULT_BIT.inv()}
        """,
        ExternalLabel("original", protonSwitch.getInstruction(0)),
    )
}

private fun trackColorFields(switchColors: ClassDef): Pair<FieldReference, FieldReference> {
    val constructor = switchColors.methods.single { it.isSwitchColorsConstructor() }
    val implementation = constructor.implementation!!
    val firstArgumentRegister = implementation.registerCount - SWITCH_COLORS_ARGUMENT_COUNT * 2
    val argumentAt = HashMap<Int, Int>()
    repeat(SWITCH_COLORS_ARGUMENT_COUNT) { argumentAt[firstArgumentRegister + it * 2] = it }
    val fieldOf = HashMap<Int, FieldReference>()
    for (instruction in implementation.instructions) {
        when (instruction.opcode) {
            Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE_16 -> {
                instruction as TwoRegisterInstruction
                val source = argumentAt[instruction.registerB]
                if (source == null) argumentAt.remove(instruction.registerA) else argumentAt[instruction.registerA] = source
            }
            Opcode.IPUT_WIDE -> {
                instruction as TwoRegisterInstruction
                val argument = argumentAt[instruction.registerA] ?: continue
                fieldOf[argument] = instruction.getReference<FieldReference>()!!
            }
            else -> Unit
        }
    }
    return Pair(
        fieldOf[CHECKED_TRACK_COLOR_ARGUMENT] ?: throw PatchException("No field stores the checked track color"),
        fieldOf[UNCHECKED_TRACK_COLOR_ARGUMENT] ?: throw PatchException("No field stores the unchecked track color"),
    )
}

private fun BytecodePatchContext.injectThumbIconIntoSwitchImpl() {
    val switchImpl = switchImplFingerprint().matchSingle().method
    val parameters = switchImpl.parameterTypes.map(CharSequence::toString)
    val checkedIndex = parameters.indexOf("Z")
    val composerType = parameters[parameters.lastIndex - 1]
    val colorsIndex = parameters.indexOfFirst { isSwitchColorsType(it) }
    val (checkedTrack, uncheckedTrack) = trackColorFields(classDefBy(parameters[colorsIndex]))

    val instructions = switchImpl.instructions.toList()
    val lastUpdaterCall = instructions.indexOfLast { instruction ->
        val reference = instruction.getReference<MethodReference>()
        instruction.opcode == Opcode.INVOKE_STATIC && reference != null && reference.returnType == "V" &&
            reference.parameterTypes.size == 3 && reference.parameterTypes.any { it == composerType }
    }
    if (lastUpdaterCall < 0) throw PatchException("No node updater call in ${descriptor(switchImpl)}")
    val updater = instructions[lastUpdaterCall] as FiveRegisterInstruction
    val composerArgument = updater.getReference<MethodReference>()!!.parameterTypes.indexOfFirst { it == composerType }
    val composerRegister = listOf(updater.registerC, updater.registerD, updater.registerE)[composerArgument]

    val rememberSwitchState = mutableClassDefBy(SWITCH_THUMB_ICON_CLASS).addStaticMethod(
        "rememberSwitchState",
        parameters,
        "V",
        4 + parameters.size,
        """
            iget-wide v0, p$colorsIndex, ${descriptor(checkedTrack)}
            iget-wide v2, p$colorsIndex, ${descriptor(uncheckedTrack)}
            invoke-static { p$checkedIndex, v0, v1, v2, v3 }, $SWITCH_THUMB_ICON_CLASS->remember(ZJJ)V
            return-void
        """,
    )
    switchImpl.addInstructions(
        lastUpdaterCall + 1,
        "invoke-static/range { v$composerRegister .. v$composerRegister }, " +
            "$SWITCH_THUMB_ICON_CLASS->drawThumbIcon(Ljava/lang/Object;)V",
    )
    switchImpl.addInstructions(0, "invoke-static/range { p0 .. p${parameters.lastIndex} }, $rememberSwitchState")
}

private fun BytecodePatchContext.styleSettingsSwitchViews() {
    val match = SettingsSwitchBindingFingerprint.matchSingle()
    val castIndex = match.instructionMatches[1].index
    val switchRegister = match.method.getInstruction<OneRegisterInstruction>(castIndex).registerA
    val switchType = match.method.getInstruction(castIndex).getReference<TypeReference>()!!.type
    val drawable = "Landroid/graphics/drawable/Drawable;"

    val styleSwitch = mutableClassDefBy(VIEW_SWITCH_STYLE_CLASS).addStaticMethod(
        "styleSwitch",
        listOf(switchType),
        "V",
        3,
        """
            invoke-static { }, $MATERIAL_SWITCHES_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :stock
            invoke-static { p0 }, $VIEW_SWITCH_STYLE_CLASS->thumb(Landroid/view/View;)$drawable
            move-result-object v0
            invoke-static { p0 }, $VIEW_SWITCH_STYLE_CLASS->track(Landroid/view/View;)$drawable
            move-result-object v1
            if-eqz v0, :stock
            if-eqz v1, :stock
            invoke-virtual { p0, v0 }, $switchType->setThumbDrawable($drawable)V
            invoke-virtual { p0, v1 }, $switchType->setTrackDrawable($drawable)V
            invoke-virtual { v1 }, $drawable->getIntrinsicWidth()I
            move-result v0
            invoke-virtual { p0, v0 }, $switchType->setSwitchMinWidth(I)V
            const/4 v0, 0x0
            invoke-virtual { p0, v0 }, $switchType->setSplitTrack(Z)V
            invoke-virtual { p0 }, $switchType->refreshDrawableState()V
            :stock
            return-void
        """,
    )
    match.method.addInstructions(castIndex + 1, "invoke-static/range { v$switchRegister .. v$switchRegister }, $styleSwitch")
}

@Suppress("unused")
val materialSwitchesPatch = bytecodePatch(
    name = "Material 3 switches",
    description = "Adds check and close icons to switches.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, resourceFieldsPatch)

    execute {
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)
        addDrawIconMethod()
        styleSettingsSwitchViews()
        injectCheckedThumbColor()
        if (ProtonSwitchFingerprint.matchOrNull() != null) {
            injectThumbContent()
            injectCheckedIconColor()
        } else {
            injectThumbIconIntoSwitchImpl()
        }
    }
}
