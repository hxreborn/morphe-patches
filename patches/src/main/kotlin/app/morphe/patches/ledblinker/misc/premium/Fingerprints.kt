/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ledblinker.misc.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object LifetimeLicenseStateFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("LIFE_TIME_LICENSE", "NO_LIFE_TIME_LICENSE"),
)

internal object SetupFullVersionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Z"),
    strings = listOf("Billing setupFullVersion setupAllFunctionsAndRemoveAds "),
)
