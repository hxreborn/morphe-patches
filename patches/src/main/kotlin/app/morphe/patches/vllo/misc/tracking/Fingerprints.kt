/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object AppsFlyerStartFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/appsflyer/AppsFlyerLib;", "Landroid/content/Context;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/appsflyer/AppsFlyerLib;", name = "start"),
    ),
)
