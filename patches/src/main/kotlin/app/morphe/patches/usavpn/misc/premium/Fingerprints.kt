/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object SubscriptionStatusFingerprint : Fingerprint(
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(
        string("key_subscription_status_new"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getInt"),
    ),
)

internal object StoreInstallFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/pm/PackageManager;", name = "getInstallerPackageName"),
        string("com.android.vending"),
        string("hms"),
    ),
)
