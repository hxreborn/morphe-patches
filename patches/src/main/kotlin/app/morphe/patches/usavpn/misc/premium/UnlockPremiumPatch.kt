/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.usavpn.misc.pairip.restoreVirtualizedMethodsPatch

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks PRO servers and removes ads and the connection time limit.",
) {
    compatibleWith(AppCompatibilities.USA_VPN, AppCompatibilities.VPN_INDIA)

    dependsOn(removePairipProtectionPatch, restoreVirtualizedMethodsPatch)

    execute {
        SubscriptionStatusFingerprint.method.returnEarly(1)
        StoreInstallFingerprint.method.returnEarly(true)
    }
}
