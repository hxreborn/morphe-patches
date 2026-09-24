/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.AvailabilityResolver
import app.morphe.patcher.patch.PatchAvailability

internal val requireArm64Delta = AvailabilityResolver { _, architecture ->
    when (architecture) {
        ApkArchitecture.ARM64_V8A, ApkArchitecture.UNIVERSAL -> PatchAvailability.ENABLED
        else -> PatchAvailability.UNAVAILABLE
    }
}
