/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.infinitepainter.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro features and removes upgrade prompts.",
) {
    compatibleWith(AppCompatibilities.INFINITE_PAINTER)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    availability(requireArm64)

    execute {
        HasMasterPurchaseFingerprint.method.returnEarly(true)
    }
}
