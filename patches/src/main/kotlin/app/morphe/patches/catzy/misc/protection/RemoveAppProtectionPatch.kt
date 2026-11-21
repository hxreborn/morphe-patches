/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.protection

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.patch
import app.morphe.patches.catzy.misc.params.inlineNativeParametersPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.arm64Availability
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

val removeAppProtectionPatch = patch {
    compatibleWith(AppCompatibilities.CATZY)

    availability(arm64Availability(PatchAvailability.REQUIRED))

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        inlineNativeParametersPatch,
    )
}
