/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medal.ads

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeFeedPromotionPatch = patch(
    name = "Remove feed promotion",
    description = "Removes the Premium promotion from the clips feed.",
) {
    compatibleWith(AppCompatibilities.MEDAL)

    execute {
        PremiumPromotionRendererFingerprint.method.returnEarly()
    }
}
