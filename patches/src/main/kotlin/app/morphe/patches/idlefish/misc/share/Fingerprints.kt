/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.share

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object ShareRouteFingerprint : Fingerprint(
    definingClass = "Lcom/taobao/idlefish/router/interrupter/pre/NavInterrupterFunShare;",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Lcom/taobao/idlefish/protocol/nav/IRouteRequest;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/net/Uri;", name = "getQueryParameter"),
        methodCall(definingClass = "Landroid/net/Uri;", name = "parse"),
    ),
)
