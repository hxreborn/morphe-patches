/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.layouts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/tradingview/ChartLayouts;"

@Suppress("unused")
val unlockChartLayoutsPatch = bytecodePatch(
    name = "Unlock multi-chart layouts",
    description = "Unlocks chart layouts with up to 8 charts on one screen.",
) {
    compatibleWith(AppCompatibilities.TRADINGVIEW)
    extendWith("extensions/extension.mpe")

    execute {
        InitWebViewFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->addLayoutLimitScript(Landroid/webkit/WebView;)V",
        )
    }
}
