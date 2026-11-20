/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.billing

import app.morphe.patcher.Fingerprint

internal object ProductPurchaseFingerprint : Fingerprint(
    isStatic = false,
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf(
        "com.vblast.flipaclip.iap.removeads",
        "com.vblast.flipaclip.iap.premium",
        "com.vblast.flipaclip.iap.feature.morelayers",
    ),
)
