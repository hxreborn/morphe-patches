/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.string
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val SWITCH_COLORS_ARGUMENT_COUNT = 16
private const val SWITCH_CAST_DISTANCE = 8
private const val PAINTER_CALL_DISTANCE = 6
private const val ICON_CALL_DISTANCE = 40
private const val CHECKED_TRACK_COLOR_DEFAULT_BIT = 0x2

internal object ProtonSwitchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "L", "L", "L", "Z", "L", "L", "L", "I", "I"),
    filters = listOf(
        string("com.protonvpn.android.base.ui.ProtonSwitch (", StringComparisonType.STARTS_WITH),
    ),
)

internal fun protonSwitchColorsFingerprint(switchColorsType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = switchColorsType,
    filters = listOf(
        string("com.protonvpn.android.base.ui.<get-protonColors> (", StringComparisonType.STARTS_WITH),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL_RANGE, returnType = switchColorsType),
    ),
)

internal fun checkedTrackColorDefaultFingerprint(switchColors: MethodReference) = Fingerprint(
    definingClass = switchColors.definingClass,
    name = switchColors.name,
    parameters = switchColors.parameterTypes.map(CharSequence::toString),
    filters = listOf(
        literal(CHECKED_TRACK_COLOR_DEFAULT_BIT, listOf(Opcode.AND_INT_LIT8)),
        fieldAccess(opcode = Opcode.SGET_OBJECT, location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "J", location = MatchAfterWithin(1)),
    ),
)

internal object IconCallerFingerprint : Fingerprint(
    filters = listOf(
        resourceField(ResourceType.DRAWABLE, "ic_proton_checkmark"),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE),
            location = MatchAfterWithin(PAINTER_CALL_DISTANCE),
        ),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE),
            parameters = listOf("L", "Ljava/lang/String;", "L", "J", "L", "I", "I"),
            returnType = "V",
            location = MatchAfterWithin(ICON_CALL_DISTANCE),
        ),
    ),
)

internal fun Method.isSwitchColorsConstructor() = name == "<init>" &&
    parameterTypes.size == SWITCH_COLORS_ARGUMENT_COUNT && parameterTypes.all { it == "J" }

internal fun BytecodePatchContext.isSwitchColorsType(type: CharSequence) =
    classDefByOrNull(type.toString())?.methods?.any { it.isSwitchColorsConstructor() } == true

internal fun BytecodePatchContext.switchImplFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Z", "Z", "L", "L", "L", "L", "I"),
    custom = { method, _ -> method.parameterTypes.any { isSwitchColorsType(it) } },
)

internal fun defaultIconSizeModifierFingerprint(iconClass: String) = Fingerprint(
    definingClass = iconClass,
    name = "<clinit>",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L", "F"), returnType = "L"),
    ),
)

private fun Method.isToolbarColorLambdaConstructor() = name == "<init>" &&
    parameterTypes.size == 3 && parameterTypes[0] == "J" && parameterTypes[1] == "J" &&
    parameterTypes[2].startsWith("L")

internal object CollapsingToolbarColorFingerprint : Fingerprint(
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "J"),
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "J", location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("J", "J", "F"), returnType = "J"),
    ),
    custom = { _, classDef -> classDef.methods.any { it.isToolbarColorLambdaConstructor() } },
)

internal object SettingsSwitchBindingFingerprint : Fingerprint(
    filters = listOf(
        resourceField(ResourceType.ID, "switchButton"),
        opcode(Opcode.CHECK_CAST, MatchAfterWithin(SWITCH_CAST_DISTANCE)),
        resourceField(ResourceType.ID, "switchTitle"),
        resourceField(ResourceType.ID, "upgradeIcon"),
    ),
)
