/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ResponseHandlerFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Response body is null"),
    filters = listOf(
        methodCall(
            parameters = listOf("[B"),
            returnType = "Lcom/facebook/react/bridge/WritableMap;",
        ),
    ),
)

internal object WebViewSourceFingerprint : Fingerprint(
    name = "onAfterUpdateTransaction",
    returnType = "V",
    strings = listOf("user-agent", "about:blank"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/webkit/WebView;",
            name = "loadUrl",
            parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
        ),
    ),
)

internal object SendRequestFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/NetworkingModule;",
    name = "sendRequestInternalReal",
    returnType = "V",
    filters = listOf(
        string("string"),
        methodCall(
            definingClass = "Lcom/facebook/react/bridge/ReadableMap;",
            name = "getString",
        ),
    ),
)
