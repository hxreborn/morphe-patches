/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.freeUserCheckFingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SETTINGS_UI = "com.protonvpn.android.redesign.settings.ui"
private const val UPGRADE_ONBOARDING_ACTIVITY = "Lcom/protonvpn/android/ui/planupgrade/UpgradeOnboardingDialogActivity;"

internal object SettingRowWithIconFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "I", "Ljava/lang/String;", "L", "Ljava/lang/Integer;", "Z", "Z", "Z", "L", "L", "I", "I"),
    strings = listOf("$SETTINGS_UI.SettingRowWithIcon ("),
)

internal object SettingsValueItemFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "L", "L", "L", "L", "I"),
    strings = listOf("$SETTINGS_UI.SettingsValueItem ("),
)

internal object ActiveNotificationsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf("J", "Ljava/util/List;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/promooffers/data/ApiNotification;", name = "getStartTime"),
        methodCall(definingClass = "Lcom/protonvpn/android/promooffers/data/ApiNotification;", name = "getEndTime"),
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

private val upgradeOnboardingActivityClass = InstructionFilter { _, instruction ->
    instruction.opcode == Opcode.CONST_CLASS &&
        instruction.getReference<TypeReference>()?.type == UPGRADE_ONBOARDING_ACTIVITY
}

internal object UpgradeOnboardingLauncherFingerprint : Fingerprint(
    strings = listOf("upgrade trigger"),
    filters = listOf(upgradeOnboardingActivityClass),
)

internal fun BytecodePatchContext.launchOnboardingFingerprint() = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(newInstance(UpgradeOnboardingLauncherFingerprint.originalClassDef.type)),
)

internal object AccountSettingsViewStateToStringFingerprint : ToStringFingerprint("AccountSettingsViewState(userId=")

internal object AccountSettingsViewStateFingerprint : Fingerprint(
    classFingerprint = AccountSettingsViewStateToStringFingerprint,
    name = "<init>",
    parameters = listOf(
        "Lme/proton/core/domain/entity/UserId;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/Integer;",
        "Z",
        "Z",
        "Ljava/util/List;",
    ),
)

internal fun BytecodePatchContext.upgradeCarouselFingerprint() = freeUserCheckFingerprint(
    followingFilters = arrayOf(
        opcode(Opcode.DIV_INT_LIT8),
        opcode(Opcode.MUL_INT_LIT8, MatchAfterImmediately()),
    ),
)
