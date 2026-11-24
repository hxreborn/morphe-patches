/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.fvfileexplorer.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object BannerAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/ads/mobile/sdk/banner/AdView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L"),
    filters = listOf(
        methodCall(
            definingClass = "Lads_mobile_sdk/",
            parameters = listOf("L"),
            returnType = "Lads_mobile_sdk/",
        ),
    ),
)
