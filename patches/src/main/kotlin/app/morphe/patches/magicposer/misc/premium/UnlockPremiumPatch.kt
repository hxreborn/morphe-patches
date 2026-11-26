/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicposer.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.magicposer.shared.addMagicPoserBridge
import app.morphe.patches.magicposer.shared.magicPoserProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro and Master features. The server enforces cloud storage limits.",
) {
    dependsOn(magicPoserProtectionPatch)
    compatibleWith(AppCompatibilities.MAGIC_POSER)
    availability(requireArm64StandardInstall)

    execute {
        addMagicPoserBridge("mppremium")
    }
}
