/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.usavpn.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks PRO servers and removes ads and the connection time limit.",
) {
    compatibleWith(AppCompatibilities.USA_VPN)

    dependsOn(removePairipProtectionPatch, restoreVirtualizedMethodsPatch)

    execute {
        SubscriptionStatusFingerprint.matchSingle().method.returnEarly(1)
        StoreInstallFingerprint.matchSingle().method.returnEarly(true)
    }
}
