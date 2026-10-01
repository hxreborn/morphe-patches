/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.AvailabilityResolver
import app.morphe.patcher.patch.PatchAvailability

internal fun arm64Availability(selection: PatchAvailability) =
    AvailabilityResolver { _, architecture ->
        when (architecture) {
            ApkArchitecture.ARM64_V8A, ApkArchitecture.UNIVERSAL -> selection
            else -> PatchAvailability.UNAVAILABLE
        }
    }

internal val requireArm64 = arm64Availability(PatchAvailability.ENABLED)
