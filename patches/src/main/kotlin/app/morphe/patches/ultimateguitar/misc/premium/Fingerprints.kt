/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.premium

import app.morphe.patcher.Fingerprint

internal object ResponseReceivedFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/NetworkEventUtil;",
    name = "onResponseReceived",
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/react/bridge/ReactApplicationContext;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "I",
        "Ljava/util/Map;",
        "J",
    ),
)

internal object TextDataReceivedFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/NetworkEventUtil;",
    name = "onDataReceived",
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/react/bridge/ReactApplicationContext;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

internal object BlobResponseDataFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/blob/BlobModule\$networkingResponseHandler\$1;",
    name = "toResponseData",
    returnType = "Lcom/facebook/react/bridge/WritableMap;",
    parameters = listOf("[B"),
)
