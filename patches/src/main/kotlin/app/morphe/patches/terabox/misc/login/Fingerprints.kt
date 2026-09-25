/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.login

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object NativeAccountPageFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("na_native_account_switch"),
)

internal object OfflinePackageManifestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/dubox/drive/login/model/OfflinePkgManifestItem;",
    parameters = listOf("Ljava/io/InputStream;"),
)

internal object WebViewSetupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;"),
    strings = listOf("searchBoxJavaBridge_", "accessibilityTraversal"),
)

internal object WebViewUserAgentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/webkit/WebSettings;", name = "setUseWideViewPort"),
        methodCall(definingClass = "Landroid/webkit/WebSettings;", name = "setUserAgentString"),
    ),
)

internal object WebChromeClientFingerprint : Fingerprint(
    name = "onProgressChanged",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "I"),
    filters = listOf(methodCall(name = "onWebLoadProgress")),
)
