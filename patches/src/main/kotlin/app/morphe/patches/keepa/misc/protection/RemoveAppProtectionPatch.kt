/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.protection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val removeAppProtectionPatch = bytecodePatch(
    name = "Remove app protection",
    description = "Lets a patched build start.",
) {
    compatibleWith(AppCompatibilities.KEEPA)

    dependsOn(removePairipProtectionPatch)
}
