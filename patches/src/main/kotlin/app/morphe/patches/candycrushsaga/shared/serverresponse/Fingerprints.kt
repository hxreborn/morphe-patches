/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared.serverresponse

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object ReadDataFingerprint : Fingerprint(
    strings = listOf("readData interrupted"),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/net/URLConnection;",
            name = "getInputStream",
            returnType = "Ljava/io/InputStream;",
            parameters = emptyList(),
        ),
    ),
)
