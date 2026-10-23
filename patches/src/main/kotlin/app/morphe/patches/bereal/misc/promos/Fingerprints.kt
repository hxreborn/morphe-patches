/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.promos

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import com.android.tools.smali.dexlib2.Opcode

private const val COMPOSER = "Landroidx/compose/runtime/Composer;"
private const val MODIFIER = "Landroidx/compose/ui/Modifier;"

internal object YearRecapWidgetToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Recap2025(shouldDisplay="),
)

internal fun yearRecapWidgetConstructorFingerprint(widgetType: String) = Fingerprint(
    definingClass = widgetType,
    name = "<init>",
    parameters = listOf("Z"),
)

internal object MonthlyRecapWidgetToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("MonthlyRecap(viewState="),
)

internal fun monthlyRecapDismissedFingerprint(widgetType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        newInstance(widgetType, location = MatchAfterWithin(4)),
    ),
)

internal object ReviewSheetToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("ReviewBottomSheetDestination"),
)

internal fun reviewPromptEnabledFingerprint(sheetType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(definingClass = sheetType, name = "INSTANCE", opcode = Opcode.SGET_OBJECT),
    ),
)

internal object ProfileCompletionBannerFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("I", "L", "L", COMPOSER, MODIFIER),
    filters = listOf(resourceLiteral(ResourceType.STRING, "banner_tip_fill_profile_message")),
)

internal object NotificationPermissionBannerFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("I", "L", COMPOSER, MODIFIER),
    filters = listOf(resourceLiteral(ResourceType.STRING, "notifications_permission_timeline_banner_title")),
)

internal object MusicButtonFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("L", "Z", "L", "L", COMPOSER, "I"),
    strings = listOf("music"),
)
