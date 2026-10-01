/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.limits

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

private const val TRAVEL_REDUCTION_MINUTES = 357L
private const val FULL_TRAVEL_DURATION_MILLISECONDS = 21_600_000L

internal fun dailyUseCounterFingerprint(property: String) = Fingerprint(
    returnType = "I",
    parameters = emptyList(),
    strings = listOf(property),
)

internal object TravelDurationFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        literal(TRAVEL_REDUCTION_MINUTES),
        literal(FULL_TRAVEL_DURATION_MILLISECONDS),
    ),
)
