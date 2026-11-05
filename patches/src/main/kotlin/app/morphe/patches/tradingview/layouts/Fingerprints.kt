/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.layouts

import app.morphe.patcher.Fingerprint

internal object InitWebViewFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/core/js/standartwebview/WebViewSessionBuilder;",
    name = "initWebView",
    parameters = listOf("Lcom/tradingview/tradingviewapp/core/js/standartwebview/AdvancedWebView;"),
)
