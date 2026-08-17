/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gammascan.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object BannerAdLoadFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Z"),
    filters = listOf(
        methodCall(definingClass = "Lcom/google/android/gms/ads/BaseAdView;", name = "loadAd"),
    ),
)

internal object InterstitialAdLoadFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
            name = "load",
        ),
    ),
)

internal object NativeAdLoadFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/google/android/gms/ads/AdLoader;", name = "loadAd"),
    ),
)
