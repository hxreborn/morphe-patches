/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.protection

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.catzy.misc.params.inlineNativeParametersPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.arm64DeltaAvailability
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

val removeAppProtectionPatch = bytecodePatch(
    name = "Remove app protection",
    description = "Lets a patched build start.",
) {
    compatibleWith(AppCompatibilities.CATZY)

    availability(arm64DeltaAvailability(PatchAvailability.REQUIRED))

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        inlineNativeParametersPatch,
    )
}
