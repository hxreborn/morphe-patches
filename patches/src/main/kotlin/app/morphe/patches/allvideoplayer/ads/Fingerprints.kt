/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal val adPlacementSwitches = listOf(
    "online_splash_appOpn_pyr",
    "SplashOpnShowPyr",
    "more_app_language_ad",
    "only_show_more_app_native_exit",
    "showMoreAppOnFailedGgl",
    "Exit_dialog_ad_show_pyr",
    "language_native_bignative_show",
    "language_native_adshow",
    "only_show_more_languaged",
    "smallNativeGglAdsShowIn_HomeAct",
    "smallNativeGglAdsShowIn_FolderOpn_Act",
    "is_home_inter_show",
    "only_show_more_app_native_banner",
    "show_more_app_native_banner",
)

internal object LoadMoreAppsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    strings = listOf("MoreAppGet_Req"),
)

internal object ApplyRemoteConfigFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/google/android/gms/tasks/Task;"),
    strings = listOf("RemoteConfig_Success"),
    filters = adPlacementSwitches.map { name ->
        fieldAccess(name = name, type = "Ljava/lang/String;", opcode = Opcode.SPUT_OBJECT)
    },
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Lcom/google/android/gms/ads/MobileAds;", name = "initialize"),
    ),
)
