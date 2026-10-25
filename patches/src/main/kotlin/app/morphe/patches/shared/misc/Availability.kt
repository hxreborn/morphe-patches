/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.AvailabilityResolver
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchAvailability

private fun architectureAvailability(selection: PatchAvailability, vararg supported: ApkArchitecture) =
    AvailabilityResolver { _, architecture ->
        if (architecture == ApkArchitecture.UNIVERSAL || architecture in supported) selection else PatchAvailability.UNAVAILABLE
    }

internal fun arm64Availability(selection: PatchAvailability) =
    architectureAvailability(selection, ApkArchitecture.ARM64_V8A)

internal val requireArm64 = arm64Availability(PatchAvailability.ENABLED)

internal val requireArm = architectureAvailability(
    PatchAvailability.ENABLED,
    ApkArchitecture.ARM64_V8A,
    ApkArchitecture.ARMEABI_V7A,
)

private val arm64Required = arm64Availability(PatchAvailability.REQUIRED)

internal val requireArm64RootMount = AvailabilityResolver { installer, architecture ->
    if (installer == InstallerType.MOUNT) arm64Required.resolve(installer, architecture) else PatchAvailability.UNAVAILABLE
}

internal val requireArm64StandardInstall = AvailabilityResolver { installer, architecture ->
    if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else requireArm64.resolve(installer, architecture)
}
