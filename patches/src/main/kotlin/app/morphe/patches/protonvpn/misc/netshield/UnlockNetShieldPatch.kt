/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.netshield

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied

@Suppress("unused")
val unlockNetShieldPatch = bytecodePatch(
    name = "Unlock NetShield",
    description = "Unlocks NetShield ad and tracker blocking on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockNetShield")
        clearFreeUserCheck(NetShieldAvailabilityFingerprint)
        clearFreeUserCheck(NetShieldResetFingerprint)
    }
}
