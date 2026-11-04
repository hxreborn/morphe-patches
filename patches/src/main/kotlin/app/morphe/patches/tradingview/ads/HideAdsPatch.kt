/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides the ads and upgrade cards in the Ideas and Community feeds.",
) {
    compatibleWith(AppCompatibilities.TRADINGVIEW)

    execute {
        AreAdsAvailableFingerprint.matchSingle().method.returnEarly(false)
    }
}
