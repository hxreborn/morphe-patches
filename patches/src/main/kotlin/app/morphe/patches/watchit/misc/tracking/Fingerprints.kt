/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.tracking

import app.morphe.patcher.Fingerprint

internal object VideoAnalyticsSubmitFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("GIF89a"),
)

internal object InteractivityAnalyticsSubmitFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Interactivity Analytics Result: "),
)

internal object FacebookAppEventsRequestFingerprint : Fingerprint(
    returnType = "L",
    strings = listOf("%s/activities", "flushState"),
)
