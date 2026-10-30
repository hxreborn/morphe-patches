/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.biwenger.ads

import app.morphe.patcher.Fingerprint

internal object BridgePostMessageFingerprint : Fingerprint(
    name = "postMessage",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("Web -> App: "),
)
