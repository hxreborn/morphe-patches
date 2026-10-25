/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.settings.WidgetSettingsRowFingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.freeUserCheckFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal const val UPGRADE_ONBOARDING_ACTIVITY = "Lcom/protonvpn/android/ui/planupgrade/UpgradeOnboardingDialogActivity;"

internal fun BytecodePatchContext.settingRowWithIconFingerprint(): Fingerprint {
    val settingRow = WidgetSettingsRowFingerprint.matchSingle().instructionMatches.last()
        .instruction.getReference<MethodReference>()!!
    return Fingerprint(
        definingClass = settingRow.definingClass,
        name = settingRow.name,
        returnType = "V",
        parameters = listOf("L", "I", "Ljava/lang/String;", "L", "Ljava/lang/Integer;", "Z", "Z", "Z", "L", "L", "I", "I"),
    )
}

internal object SettingsValueItemFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "L", "L", "L", "L", "I"),
    strings = listOf("onNavigateTo", "onRestricted"),
)

internal object ApiNotificationToStringFingerprint : ToStringFingerprint("ApiNotification(id=")

private val BytecodePatchContext.apiNotificationType
    get() = ApiNotificationToStringFingerprint.originalClassDef.type

private fun BytecodePatchContext.apiNotificationTimeCall() = methodCall(
    definingClass = apiNotificationType,
    parameters = emptyList(),
    returnType = "J",
)

internal fun BytecodePatchContext.activeNotificationsFingerprint() = Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf("J", "Ljava/util/List;"),
    filters = listOf(apiNotificationTimeCall(), apiNotificationTimeCall()),
)

internal fun BytecodePatchContext.inlinedActiveNotificationsFingerprint() = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        apiNotificationTimeCall(),
        apiNotificationTimeCall(),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/Iterable;", "Ljava/util/Comparator;"),
            returnType = "Ljava/util/List;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object ServerGroupBannerToStringFingerprint : ToStringFingerprint("Banner(type=")

internal object FreeConnectionsUpsellBannerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceField(ResourceType.STRING, "free_connections_info_banner_text"),
        methodCall(returnType = "Landroidx/constraintlayout/widget/ConstraintLayout;", location = MatchAfterWithin(3)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object InlinedFreeConnectionsUpsellBannerFingerprint : Fingerprint(
    filters = listOf(
        resourceField(ResourceType.STRING, "free_connections_info_banner_text"),
        checkCast("Landroidx/constraintlayout/widget/ConstraintLayout;", MatchAfterWithin(5)),
    ),
)

private val upgradeOnboardingActivityClass = InstructionFilter { _, instruction ->
    instruction.opcode == Opcode.CONST_CLASS &&
        instruction.getReference<TypeReference>()?.type == UPGRADE_ONBOARDING_ACTIVITY
}

internal object UpgradeOnboardingLauncherFingerprint : Fingerprint(
    strings = listOf("upgrade trigger"),
    filters = listOf(upgradeOnboardingActivityClass),
)

internal fun BytecodePatchContext.launchOnboardingFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(newInstance(UpgradeOnboardingLauncherFingerprint.originalClassDef.type)),
)

private val switchOpcodes = setOf(Opcode.PACKED_SWITCH, Opcode.SPARSE_SWITCH)

private val BytecodePatchContext.onboardingLaunchKey: Int
    get() {
        val instructions = UpgradeOnboardingLauncherFingerprint.originalMethod.implementation!!.instructions.toList()
        val addresses = instructions.runningFold(0) { address, instruction -> address + instruction.codeUnits }
        val switchAddress = addresses[instructions.indexOfFirst { it.opcode in switchOpcodes }]
        val launchAddress = addresses[UpgradeOnboardingLauncherFingerprint.instructionMatches.first().index]
        val cases = instructions.filterIsInstance<SwitchPayload>().single().switchElements
        val launchCase = cases.filter { switchAddress + it.offset <= launchAddress }.maxByOrNull { it.offset }
        return launchCase?.key ?: (cases.maxOf { it.key } + 1)
    }

internal fun BytecodePatchContext.onboardingLaunchCoroutineFingerprint() = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        newInstance(UpgradeOnboardingLauncherFingerprint.originalClassDef.type),
        literal(onboardingLaunchKey, location = MatchAfterWithin(2)),
    ),
)

internal object UpgradeDialogCreateFingerprint : Fingerprint(
    definingClass = "/UpgradeDialogActivityV2;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(methodCall(name = "onCreate", parameters = listOf("Landroid/os/Bundle;"))),
)

internal object AccountSettingsViewStateToStringFingerprint : ToStringFingerprint("AccountSettingsViewState(userId=")

internal fun BytecodePatchContext.upgradeCarouselFingerprint() = freeUserCheckFingerprint(
    followingFilters = arrayOf(
        opcode(Opcode.DIV_INT_LIT8),
        opcode(Opcode.MUL_INT_LIT8, MatchAfterImmediately()),
    ),
)
