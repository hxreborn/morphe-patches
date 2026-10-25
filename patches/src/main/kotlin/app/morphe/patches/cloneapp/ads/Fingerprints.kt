/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val CONSENT_INFORMATION_CLASS = "Lcom/google/android/ump/ConsentInformation;"

internal object SplashAdEligibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("SPLASH_AD_NEW_USER"),
)

internal object OtherAdEligibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("OTHER_AD_NEW_USER"),
)

internal object AdConsentAndInitFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(
        methodCall(definingClass = CONSENT_INFORMATION_CLASS, name = "requestConsentInfoUpdate"),
        methodCall(definingClass = CONSENT_INFORMATION_CLASS, name = "canRequestAds"),
    ),
)

internal object EmbeddedAdsFlagFingerprint : Fingerprint(
    name = "isShowEmbraveAds",
    returnType = "Z",
    parameters = emptyList(),
)
