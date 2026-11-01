/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object HasAdRemovalFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("remove.ads.purchased", "legacy.ads.removal.owned"),
)

internal object RequestAdConsentFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/ump/ConsentInformation;",
            name = "requestConsentInfoUpdate",
        ),
    ),
)
