/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.prayertimes.misc.protection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val removeAppProtectionPatch = bytecodePatch(
    name = "Remove app protection",
    description = "Lets a patched build of the Wear OS app start. " +
        "Syncing with the phone app requires both apps patched with the same signing key.",
) {
    compatibleWith(AppCompatibilities.PRAYER_TIMES_WEAR)

    dependsOn(removePairipProtectionPatch)
}
