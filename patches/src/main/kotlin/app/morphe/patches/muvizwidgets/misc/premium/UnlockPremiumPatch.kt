/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.muvizwidgets.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro widgets and styles and removes ads.",
) {
    compatibleWith(AppCompatibilities.MUVIZ_WIDGETS)

    dependsOn(removePairipProtectionPatch)

    execute {
        IsProPurchasedFingerprint.matchSingle().method.returnEarly(true)
    }
}
