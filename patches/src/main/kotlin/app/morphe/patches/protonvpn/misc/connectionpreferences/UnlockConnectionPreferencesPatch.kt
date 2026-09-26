/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.unlockUserSetting
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockConnectionPreferencesPatch = bytecodePatch(
    name = "Unlock connection preferences",
    description = "Unlocks the default connection and excluded locations on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)

    execute {
        unlockUserSetting(
            ConnectionPreferencesViewStateFingerprint,
            freeUserParameter = 2,
            DefaultConnectionRestrictionFingerprint,
        )
    }
}
