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
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.string
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SWITCH_CAST_DISTANCE = 8
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

internal object PainterResourceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("I", "L", "I"),
    filters = listOf(
        string("androidx.compose.ui.res.painterResource (", StringComparisonType.STARTS_WITH),
    ),
)

internal fun painterIconFingerprint(painterType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(painterType, "Ljava/lang/String;", "L", "J", "L", "I", "I"),
    filters = listOf(
        string("androidx.compose.material3.Icon (", StringComparisonType.STARTS_WITH),
    ),
)

internal fun defaultIconSizeModifierFingerprint(iconClass: String) = Fingerprint(
    definingClass = iconClass,
    name = "<clinit>",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L", "F"), returnType = "L"),
    ),
)

internal object SettingsSwitchBindingFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        resourceField(ResourceType.ID, "switchButton"),
        opcode(Opcode.CHECK_CAST, MatchAfterWithin(SWITCH_CAST_DISTANCE)),
        resourceField(ResourceType.ID, "switchTitle"),
        resourceField(ResourceType.ID, "upgradeIcon"),
    ),
)
