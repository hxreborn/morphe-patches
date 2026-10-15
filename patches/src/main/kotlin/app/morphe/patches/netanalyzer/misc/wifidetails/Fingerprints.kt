/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.wifidetails

import app.morphe.patcher.Fingerprint
import app.morphe.patches.netanalyzer.shared.composable

internal const val WIFI_SIGNAL_ROW = "WifiSignalRowComposable"

internal object WifiSignalRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(WIFI_SIGNAL_ROW)),
)
