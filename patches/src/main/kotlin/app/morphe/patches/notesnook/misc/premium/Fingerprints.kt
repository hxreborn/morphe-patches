/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notesnook.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object ResponseHandlerFingerprint : Fingerprint(
    name = "onResponse",
    returnType = "V",
    parameters = listOf("Lokhttp3/Call;", "Lokhttp3/Response;"),
    filters = listOf(
        methodCall(
            definingClass = "/NetworkingModule\$ResponseHandler;",
            name = "toResponseData",
            parameters = listOf("[B"),
            returnType = "Lcom/facebook/react/bridge/WritableMap;",
        ),
    ),
)
