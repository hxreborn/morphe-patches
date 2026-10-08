/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.addStaticMethod
import app.morphe.patches.shared.misc.proton.descriptor
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.replaceStub
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
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
private const val ICON_TINT_DEFAULT_BIT = 0x8
private const val SWITCH_ICON_SIZE_DP = 16f

private fun BytecodePatchContext.addDrawIconMethod() {
    val checkmarkId = resourceId(ResourceType.DRAWABLE, "ic_proton_checkmark")
    val crossId = resourceId(ResourceType.DRAWABLE, "ic_proton_cross")

    val painterResource = PainterResourceFingerprint.matchSingle().originalMethod
    val icon = painterIconFingerprint(painterResource.returnType).matchSingle().originalMethod
    val composerType = icon.parameterTypes[4].toString()
    val modifierType = icon.parameterTypes[2].toString()

    val defaultIconSize = defaultIconSizeModifierFingerprint(icon.definingClass).matchSingle()
    val companion = defaultIconSize.method
        .getInstruction(defaultIconSize.instructionMatches.first().index)
        .getReference<FieldReference>()!!
    val size = defaultIconSize.method
        .getInstruction(defaultIconSize.instructionMatches.last().index)
        .getReference<MethodReference>()!!
    if (companion.definingClass != modifierType || size.parameterTypes.first().toString() != modifierType) {
        throw PatchException("Default icon size does not read $modifierType: ${descriptor(companion)}, ${descriptor(size)}")
    }

    mutableClassDefBy(SWITCH_THUMB_ICON_CLASS).replaceStub(
        "drawIcon",
        10,
        """
            if-eqz p1, :unchecked
            const v0, $checkmarkId
            goto :draw
            :unchecked
            const v0, $crossId
            :draw
            check-cast p0, $composerType
            const/4 v1, 0x0
            invoke-static { v0, p0, v1 }, ${descriptor(painterResource)}
            move-result-object v0
            sget-object v2, ${descriptor(companion)}
            const v3, ${SWITCH_ICON_SIZE_DP.toRawBits()}
            invoke-static { v2, v3 }, ${descriptor(size)}
            move-result-object v2
            const-wide/16 v3, 0x0
            move-object v5, p0
            const/4 v6, 0x0
            const/16 v7, $ICON_TINT_DEFAULT_BIT
            invoke-static/range { v0 .. v7 }, ${descriptor(icon)}
            return-void
        """,
    )
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

private fun BytecodePatchContext.injectThumbContent() {
    val protonSwitch = ProtonSwitchFingerprint.matchSingle().method
    val thumbContentType = protonSwitch.parameterTypes[THUMB_CONTENT_PARAMETER].toString()
    val defaultMask = "p${protonSwitch.parameterTypes.lastIndex}"

    protonSwitch.addInstructionsWithLabels(
        0,
        """
            and-int/lit8 v0, $defaultMask, $THUMB_CONTENT_DEFAULT_BIT
            if-nez v0, :inject
            if-nez p$THUMB_CONTENT_PARAMETER, :original
            :inject
            const-class v0, $thumbContentType
            move/from16 v1, p0
            invoke-static { v0, v1 }, $SWITCH_THUMB_ICON_CLASS->thumbContent(Ljava/lang/Class;Z)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, $thumbContentType
            move-object/from16 p$THUMB_CONTENT_PARAMETER, v0
            and-int/lit8 $defaultMask, $defaultMask, ${THUMB_CONTENT_DEFAULT_BIT.inv()}
        """,
        ExternalLabel("original", protonSwitch.getInstruction(0)),
    )
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
        injectThumbContent()
        injectCheckedIconColor()
    }
}
