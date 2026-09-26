/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.upselling

import app.morphe.patcher.Fingerprint

internal object SettingRowWithIconFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/redesign/settings/ui/SettingsKt;",
    name = "SettingRowWithIcon",
    returnType = "V",
    parameters = listOf(
        "Landroidx/compose/ui/Modifier;",
        "I",
        "Ljava/lang/String;",
        "Lcom/protonvpn/android/redesign/settings/ui/SettingValue;",
        "Ljava/lang/Integer;",
        "Z",
        "Z",
        "Z",
        "Lkotlin/jvm/functions/Function0;",
        "Landroidx/compose/runtime/Composer;",
        "I",
        "I",
    ),
)

internal object SettingsValueItemFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/redesign/settings/ui/AdvancedSettingsKt;",
    name = "SettingsValueItem",
    returnType = "V",
    parameters = listOf(
        "Lcom/protonvpn/android/redesign/settings/ui/SettingsViewModel\$SettingViewState;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;",
        "Landroidx/compose/runtime/Composer;",
        "I",
    ),
)

internal object ActiveNotificationsFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/promooffers/data/ApiNotificationManager;",
    name = "activeNotifications",
    returnType = "Ljava/util/List;",
    parameters = listOf("J", "Ljava/util/List;"),
)

internal object ServerGroupsMainScreenStateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/redesign/countries/ui/ServerGroupsMainScreenState;",
    name = "<init>",
    parameters = listOf(
        "Lcom/protonvpn/android/redesign/countries/ui/ServerFilterType;",
        "Ljava/util/List;",
        "Ljava/util/List;",
    ),
)

internal object FreeConnectionsInfoFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/ui/home/FreeConnectionsInfoBottomSheetKt;",
    name = "setupViews",
    returnType = "V",
)

internal object LaunchOnboardingFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/ui/planupgrade/UpgradeDialogLauncherVM;",
    name = "launchOnboarding",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)
