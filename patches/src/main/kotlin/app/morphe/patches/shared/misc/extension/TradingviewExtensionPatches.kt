/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val tradingviewDisableAnalyticsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/tradingview-disable-analytics.mpe")
}

internal val tradingviewUnlockChartFeaturesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/tradingview-unlock-chart-features.mpe")
}
