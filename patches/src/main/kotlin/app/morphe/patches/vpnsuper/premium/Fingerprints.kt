/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val SETTINGS_DATA_STORE_CLASS =
    "Lcom/superunlimited/feature/settings/domain/entities/SettingsDataStore;"

internal object VipStatusFlowFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(definingClass = SETTINGS_DATA_STORE_CLASS, opcode = Opcode.SGET_OBJECT),
    ),
)

internal object ConnectReportToStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(
        "ConnectReportViewState(shouldAttemptLaunchingInAppReviewFlow=",
        ", showBannerAds=",
        ", showBannerRateUs=",
        ", freeTrial=",
    ),
)

internal object PrivacyPolicyAcceptedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "user_tap_agree_to_privacy_policy",
        "billing_iap_page_enter_from_launch",
        "user_tap_accepted_tos_update",
    ),
)

private const val USER_STATE_LABEL = ", userState="
private const val MULTI_PLATFORM_LABEL = ", isMpfEnabled="
private const val USER_INFO_FIELD_READ_WINDOW = 1

internal object UserInfoToStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("UserInfo(email=", ", isPremium=", USER_STATE_LABEL, ", tokenStatus="),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        string(USER_STATE_LABEL),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(USER_INFO_FIELD_READ_WINDOW)),
        string(MULTI_PLATFORM_LABEL),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, location = MatchAfterWithin(USER_INFO_FIELD_READ_WINDOW)),
    ),
)

internal object TvEverythingLoadedFingerprint : Fingerprint(
    strings = listOf("TvOnEverythingIsLoadedMsg"),
)

internal object TvSplashDestinationFingerprint : Fingerprint(
    classFingerprint = TvEverythingLoadedFingerprint,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)

internal object CrownClickFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        string("home_tap_crown"),
        string("billing_iap_page_enter_from_crown"),
    ),
)
