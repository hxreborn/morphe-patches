/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AppOpenAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/appopen/AppOpenAd;",
    name = "load",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
)

internal object InterstitialAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
    name = "load",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
)

internal object AdLoaderWithAdListenerFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/AdLoader\$Builder;",
    name = "withAdListener",
    parameters = listOf("Lcom/google/android/gms/ads/AdListener;"),
)

internal object AdLoaderLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/AdLoader;",
    returnType = "V",
    custom = { method, _ -> method.name == "loadAd" || method.name == "loadAds" },
)
