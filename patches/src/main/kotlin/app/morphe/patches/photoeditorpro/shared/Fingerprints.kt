/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object PurchasePreferencesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/SharedPreferences\$OnSharedPreferenceChangeListener;"),
)

internal object RemoveAdsPurchasedFingerprint : Fingerprint(
    classFingerprint = PurchasePreferencesFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Landroid/content/SharedPreferences;",
        ),
        methodCall(
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getBoolean",
        ),
    ),
)

internal object ProGateFingerprint : Fingerprint(
    classFingerprint = RemoveAdsPurchasedFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getBoolean",
        ),
    ),
)
