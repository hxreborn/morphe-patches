/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.chart

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.tradingviewUnlockChartFeaturesExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/tradingview/ChartPlanLimits;"

@Suppress("unused")
val unlockChartFeaturesPatch = bytecodePatch(
    name = "Unlock chart features",
    description = "Unlocks layouts with up to 8 charts, more than 3 indicators per chart and the Renko, Kagi, Point & Figure, Line break, Range, Volume candles and HLC area chart types. Alerts, watchlists, profile indicators and second or tick intervals follow your plan.",
) {
    dependsOn(tradingviewUnlockChartFeaturesExtensionPatch)
    compatibleWith(AppCompatibilities.TRADINGVIEW)
    execute {
        InitWebViewFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->addPlanLimitsScript(Landroid/webkit/WebView;)V",
        )
    }
}
