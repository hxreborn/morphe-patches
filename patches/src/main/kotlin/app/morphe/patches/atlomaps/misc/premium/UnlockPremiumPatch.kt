/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atlomaps.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the custom map sources, navigation settings and backup restore. " +
        "Premium map packages are not included.",
) {
    compatibleWith(AppCompatibilities.ATLOMAPS)

    execute {
        OwnsFeatureFingerprint.matchSingle().method.returnEarly(true)
        PremiumEnabledFingerprint.matchSingle().method.returnEarly(true)
    }
}
