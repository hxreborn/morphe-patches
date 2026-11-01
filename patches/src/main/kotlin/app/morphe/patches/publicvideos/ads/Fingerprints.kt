/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object InitializeMobileAdsFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(definingClass = "Ljava/lang/Boolean;", name = "TRUE", opcode = Opcode.SGET_OBJECT),
        string("ca-app-pub-4482405035205548~9657278803"),
    ),
)

internal object RegisterAmazonAdsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Lcom/amazon/device/ads/AdRegistration;", name = "getInstance"),
        methodCall(definingClass = "Lcom/amazon/device/ads/AdRegistration;", name = "setAdNetworkInfo"),
    ),
)

internal object InitializeAudienceNetworkFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Lcom/facebook/ads/AudienceNetworkAds;", name = "initialize"),
    ),
)

internal object SplashForegroundImageFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("SPLASH_SCREEN_IMAGE_URL"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getString"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)
