/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShouldShowAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("shouldShowAd, supportPreviewAd"),
)

internal object ConsentSdkAvailabilityFingerprint : Fingerprint(
    definingClass = "Lcom/proxy/ad/cmp/GoogleCmpHelper;",
    name = "hasCmpSDK",
    returnType = "Z",
)
