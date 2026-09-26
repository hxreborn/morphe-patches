/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.netshield

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.treatAsPaidUser
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockNetShieldPatch = bytecodePatch(
    name = "Unlock NetShield",
    description = "Unlocks NetShield ad and tracker blocking on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)

    execute {
        treatAsPaidUser(NetShieldAvailabilityFingerprint)
        treatAsPaidUser(NetShieldResetFingerprint)
    }
}
