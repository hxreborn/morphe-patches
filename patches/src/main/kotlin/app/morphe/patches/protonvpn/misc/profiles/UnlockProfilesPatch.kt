/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.treatAsPaidUser
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockProfilesPatch = bytecodePatch(
    name = "Unlock profiles",
    description = "Unlocks profiles on free plans. Profiles for locations without free servers open the upgrade screen.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)

    execute {
        treatAsPaidUser(ProfileAvailabilityFingerprint)
    }
}
