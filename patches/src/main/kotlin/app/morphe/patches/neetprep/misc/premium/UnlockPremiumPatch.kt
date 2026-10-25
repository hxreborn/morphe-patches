/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.neetprep.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks question and test explanations, Ask AI and attempt deletion.",
) {
    compatibleWith(AppCompatibilities.NEETPREP)

    dependsOn(removePairipProtectionPatch)

    execute {
        UserPaidFingerprint.matchSingle().method.returnEarly(true)
    }
}
