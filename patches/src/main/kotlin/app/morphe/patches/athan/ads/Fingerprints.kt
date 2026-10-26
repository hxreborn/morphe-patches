/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object InterstitialBeforeActionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Z", "Lkotlin/jvm/functions/Function0;"),
    strings = listOf("ad_pushup_enable", "shouldShowInterstitialAd()"),
)
