/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.telemetry

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AppsFlyerStartFingerprint : Fingerprint(
    definingClass = "Lcom/oneweather/common/others/AppsFlyerSdk;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/oneweather/common/preference/CommonPrefManager;",
        "Ljava/lang/String;",
        "Z",
        "Lcom/appsflyer/AppsFlyerConversionListener;",
        "Lcom/oneweather/flavour/FlavourManager;",
    ),
)
