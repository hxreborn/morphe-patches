/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicoletcastplugin.misc.protection

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

@Suppress("unused")
val removeAppProtectionPatch = patch(
    name = "Remove app protection",
    description = "Lets a patched build start. " +
        "Cast in Musicolet requires both apps patched with the same signing key.",
) {
    compatibleWith(AppCompatibilities.MUSICOLET_CAST_PLUGIN)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
}
