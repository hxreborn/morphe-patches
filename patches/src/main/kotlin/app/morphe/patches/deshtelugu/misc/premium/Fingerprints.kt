/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.deshtelugu.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object PremiumPurchaseQueryFingerprint : Fingerprint(
    returnType = "Lcom/android/billingclient/api/Purchase;",
    parameters = listOf("Ljava/util/List;"),
    filters = listOf(
        string("premium"),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "valueOf"),
        string("pref_manglish"),
        methodCall(definingClass = "Landroid/content/SharedPreferences\$Editor;", name = "putBoolean"),
    ),
)
