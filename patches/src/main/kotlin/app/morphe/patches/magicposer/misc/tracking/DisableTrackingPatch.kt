/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicposer.misc.tracking

import app.morphe.patcher.patch.patch
import app.morphe.patches.magicposer.shared.addMagicPoserBridge
import app.morphe.patches.magicposer.shared.magicPoserProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops analytics events, sessions and crash reports from being sent.",
) {
    dependsOn(magicPoserProtectionPatch)
    compatibleWith(AppCompatibilities.MAGIC_POSER)
    availability(requireArm64StandardInstall)

    execute {
        addMagicPoserBridge("mptracking")
    }
}
