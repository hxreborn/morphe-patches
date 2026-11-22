/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atlomaps.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks the custom map sources, navigation settings and backup restore. " +
        "Premium map packages are not included.",
) {
    compatibleWith(AppCompatibilities.ATLOMAPS)

    execute {
        OwnsFeatureFingerprint.method.returnEarly(true)
        PremiumEnabledFingerprint.method.returnEarly(true)
    }
}
