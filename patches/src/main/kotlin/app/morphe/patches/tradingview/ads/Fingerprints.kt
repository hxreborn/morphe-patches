/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.ads

import app.morphe.patcher.Fingerprint

internal object AreAdsAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/feature/ads/impl/entity/AdsInitializationEntity;",
    name = "areAdsAvailable",
    returnType = "Z",
    parameters = listOf(
        "Z",
        "Lcom/tradingview/tradingviewapp/feature/ads/api/model/toggle/AdsConfigToggleInfo;",
        "J",
        "J",
    ),
)
