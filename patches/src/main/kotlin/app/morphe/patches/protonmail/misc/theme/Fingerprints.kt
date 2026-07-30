/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

internal val DARK_BACKGROUND_COLORS = listOf(
    0xFF191927L, // mailbox, message and composer
    0xFF222230L, // settings and bottom sheets
)

internal object DarkPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(DARK_BACKGROUND_COLORS.first())),
)

internal object UpsellingDarkBackgroundFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(literal(DARK_BACKGROUND_COLORS.first())),
)

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
