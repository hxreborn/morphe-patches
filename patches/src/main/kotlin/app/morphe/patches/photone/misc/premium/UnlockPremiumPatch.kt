/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photone.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all light sources, extended PAR, Pro guides, Pro settings and the full toolbox. " +
        "Pro support is not included.",
) {
    compatibleWith(AppCompatibilities.PHOTONE)

    execute {
        HasProProductFingerprint.matchSingle().method.returnEarly(true)
    }
}
