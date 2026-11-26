/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.exiftool.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.checkCast
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

internal object BannerLoadFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Landroid/view/ViewGroup;"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "ads_admob_id_banner")),
)

internal object NativePreloadFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "ads_admob_id_native")),
)

internal object NativeLoadFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/view/ViewGroup;"),
    filters = listOf(checkCast("Lcom/google/android/libraries/ads/mobile/sdk/nativead/NativeAdView;")),
)

internal object AppOpenPreloadFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(resourceLiteral(ResourceType.STRING, "ads_admob_id_app_open")),
)

internal object InterstitialPreloadFingerprint : Fingerprint(
    definingClass = "Lcom/exiftool/free/base/BaseActivity;",
    isStatic = false,
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(resourceLiteral(ResourceType.STRING, "ads_admob_id_interstitial")),
)

internal object InterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/exiftool/free/base/BaseActivity;",
    isStatic = false,
    returnType = "V",
    filters = listOf(resourceLiteral(ResourceType.STRING, "ads_admob_id_interstitial")),
    custom = { method, _ -> method.parameterTypes.size == 1 },
)
