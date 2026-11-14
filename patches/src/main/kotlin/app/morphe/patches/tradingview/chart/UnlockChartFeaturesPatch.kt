/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.chart

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/tradingview/ChartPlanLimits;"

@Suppress("unused")
val unlockChartFeaturesPatch = bytecodePatch(
    name = "Unlock chart features",
    description = "Unlocks layouts with up to 8 charts, a third indicator per chart and the Renko, Kagi, Point & Figure, Line break, Range, Volume candles and HLC area chart types. Alerts, watchlists, profile indicators and second or tick intervals stay on your plan.",
) {
    compatibleWith(AppCompatibilities.TRADINGVIEW)
    extendWith("extensions/extension.mpe")

    execute {
        InitWebViewFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->addPlanLimitsScript(Landroid/webkit/WebView;)V",
        )
    }
}
