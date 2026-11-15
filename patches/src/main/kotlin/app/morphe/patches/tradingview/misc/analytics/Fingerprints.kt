/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.analytics

import app.morphe.patcher.Fingerprint

internal object NetworkTelemetryClassFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("oneThreadScope", "singleThreadIOScope"),
)

internal object NetworkTelemetryInitializationFingerprint : Fingerprint(
    classFingerprint = NetworkTelemetryClassFingerprint,
    returnType = "V",
    parameters = listOf("Z"),
    isStatic = false,
)

internal object SnowPlowTrackerInitializationFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("tradingview_android"),
)

internal object ChartWebViewClientFingerprint : Fingerprint(
    superclass = "Landroid/webkit/WebViewClient;",
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
    strings = listOf("webViewClient: onPageFinished, url: "),
)
