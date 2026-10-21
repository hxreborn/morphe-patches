/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.glasswidgets.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipKeyImportPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro widgets, glass styles and settings and removes ads.",
) {
    compatibleWith(AppCompatibilities.GLASS_WIDGETS)

    dependsOn(removePairipKeyImportPatch, removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        PremiumCheckFingerprint.matchSingle().method.returnEarly(true)
    }
}
