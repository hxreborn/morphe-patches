/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val tradingviewDisableAnalyticsExtensionPatch = patch {
    extendWith("extensions/hxreborn/tradingview-disable-analytics.mpe")
}

internal val tradingviewUnlockChartFeaturesExtensionPatch = patch {
    extendWith("extensions/hxreborn/tradingview-unlock-chart-features.mpe")
}
