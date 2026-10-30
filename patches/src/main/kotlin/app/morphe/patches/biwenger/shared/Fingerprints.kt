/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.biwenger.shared

import app.morphe.patcher.Fingerprint

private const val WEB_VIEW_CLASS = "Lcom/app/webview/Web/AdvancedWebView;"

internal object PageStartedFingerprint : Fingerprint(
    name = "onPageStarted",
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;", "Landroid/graphics/Bitmap;"),
    custom = { _, classDef -> classDef.fields.any { it.type == WEB_VIEW_CLASS } },
)
