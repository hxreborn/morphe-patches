/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.tracking

import app.morphe.patcher.Fingerprint

internal object FacebookOpenConnectionFingerprint : Fingerprint(
    returnType = "Ljava/net/HttpURLConnection;",
    parameters = listOf("Ljava/net/URL;"),
    strings = listOf("FBAndroidSDK", "User-Agent"),
)
