/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.filmish.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ProStateInitializerFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/SharedPreferences;"),
    filters = listOf(
        string("settings.isPro"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getBoolean"),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "valueOf", parameters = listOf("Z")),
    ),
)

internal object PurchaseRefreshFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("com.filmish.pro", "settings.isPro"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "isEmpty", returnType = "Z"),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "valueOf", parameters = listOf("Z")),
    ),
)
