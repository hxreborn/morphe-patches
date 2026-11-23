/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.ai

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object WebViewSetupFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/business/widget/webview/BaseWebViewFragment;",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(methodCall(definingClass = "Landroid/webkit/WebView;", name = "setDownloadListener")),
)

internal object WebPageFinishedFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/business/widget/webview/client/CommonWebClient;",
    name = "onPageFinished",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
)
