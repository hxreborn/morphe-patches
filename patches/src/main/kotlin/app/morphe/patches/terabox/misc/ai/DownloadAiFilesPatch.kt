/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.ai

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.teraboxDownloadAiFilesExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/AiGeneratedDownload;"

@Suppress("unused")
val downloadAiFilesPatch: Patch = patch(
    name = "Download generated files",
    description = "Downloads files generated in TeraBox chat to the Downloads folder.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(teraboxDownloadAiFilesExtensionPatch)

    execute {
        WebViewSetupFingerprint.match().method.addInstructions(
            0,
            """
                iget-object v0, p0, Lcom/dubox/drive/business/widget/webview/BaseWebViewFragment;->mWebView:Lcom/dubox/drive/business/widget/webview/DuboxWebView;
                invoke-static { v0 }, $EXTENSION_CLASS->attach(Landroid/webkit/WebView;)V
            """,
        )
        WebPageFinishedFingerprint.match().method.addInstructions(
            0,
            "invoke-static { p1, p2 }, $EXTENSION_CLASS->onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V",
        )
    }
}
