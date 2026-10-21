/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object AppMetricaActivateFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lio/appmetrica/analytics/AppMetrica;",
            name = "activate",
        ),
    ),
)
