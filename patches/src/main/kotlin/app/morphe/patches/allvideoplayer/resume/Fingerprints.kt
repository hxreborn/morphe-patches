/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.resume

import app.morphe.patcher.Fingerprint

internal object PlayVideoFingerprint : Fingerprint(
    definingClass = "Lcom/allformatplayer/streamvideoplayer/feature/player/MyPlayerActivity;",
    parameters = listOf("Landroid/net/Uri;"),
    custom = { method, _ -> method.returnType != "V" },
)
