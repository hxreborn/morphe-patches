/*
 * Copyright (C) 2026 Bogat25
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pocketwhip.ads

import app.morphe.patcher.Fingerprint

/**
 * `BaseAdView.loadAd(AdRequest)` of the Google Mobile Ads SDK. Every banner `AdView` loads through it.
 * Public SDK API, so its name does not change with app updates or obfuscation.
 */
internal object BannerLoadAdFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/BaseAdView;",
    name = "loadAd",
    returnType = "V",
    parameters = listOf("Lcom/google/android/gms/ads/AdRequest;"),
)

internal object MobileAdsInitializeFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/MobileAds;",
    name = "initialize",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;",
    ),
)

internal object RewardedAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/rewarded/RewardedAd;",
    name = "load",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Lcom/google/android/gms/ads/AdRequest;",
        "Lcom/google/android/gms/ads/rewarded/RewardedAdLoadCallback;",
    ),
)
