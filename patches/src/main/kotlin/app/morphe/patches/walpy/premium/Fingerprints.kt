/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.walpy.premium

import app.morphe.patcher.Fingerprint

internal object SubscriptionPurchaseFingerprint : Fingerprint(
    strings = listOf("walpy_pro"),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
)
