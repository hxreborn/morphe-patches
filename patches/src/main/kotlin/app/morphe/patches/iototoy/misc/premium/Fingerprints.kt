/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iototoy.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object IsProductPurchasedFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/android/billingclient/api/Purchase;", returnType = "Ljava/util/ArrayList;"),
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "contains"),
        string("purchaseState"),
    ),
)
