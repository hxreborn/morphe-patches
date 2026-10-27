/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.protection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val removeAppProtectionPatch = bytecodePatch(
    name = "Remove app protection",
    description = "Lets a patched build start.",
) {
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
}
