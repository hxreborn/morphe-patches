/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme.webview

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object CachedMessageBodyFingerprint : Fingerprint(
    name = "shouldInterceptRequest",
    returnType = "Landroid/webkit/WebResourceResponse;",
    strings = listOf("text/html", "utf-8"),
)

internal object InlineMessageBodyFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "message-webview: setting initial value on webview ",
        "https://ch.proton.local.body.host/body/",
    ),
)

internal object ComposerCssFingerprint : Fingerprint(
    strings = listOf("Raw css resource is not found"),
    filters = listOf(
        methodCall("Landroid/content/res/Resources;->openRawResource(I)Ljava/io/InputStream;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object WebSettingsClientFingerprint : Fingerprint(
    strings = listOf("web-settings: onReceivedSslError: "),
)

internal object WebSettingsPageFinishedFingerprint : Fingerprint(
    classFingerprint = WebSettingsClientFingerprint,
    name = "onPageFinished",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
)

internal object WebSettingsCreatedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Z",
        "Lch/protonmail/android/mailsettings/presentation/websettings/WebSettingsJavaScriptInterface;",
        "Landroid/webkit/WebView;",
    ),
)
