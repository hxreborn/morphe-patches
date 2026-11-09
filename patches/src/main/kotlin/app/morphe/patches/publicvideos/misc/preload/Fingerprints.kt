/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.preload

import app.morphe.patcher.Fingerprint

internal object PreloadManagerFingerprint : Fingerprint(
    strings = listOf("DefaultPreloadManager wasn't configured with a Cache"),
)

internal object PreloadManagerAddFingerprint : Fingerprint(
    classFingerprint = PreloadManagerFingerprint,
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Integer;"),
)
