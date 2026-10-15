/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.prayertimes.misc.premium

import app.morphe.patcher.Fingerprint

internal object IsProUnlockedFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("cool_date", "cool_zero_00"),
)
