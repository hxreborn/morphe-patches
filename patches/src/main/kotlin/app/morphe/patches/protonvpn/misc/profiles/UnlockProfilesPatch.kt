/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied

@Suppress("unused")
val unlockProfilesPatch = bytecodePatch(
    name = "Unlock profiles",
    description = "Unlocks profiles on free plans. Profiles for locations without free servers open the upgrade screen.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockProfiles")
        clearFreeUserCheck(ProfileAvailabilityFingerprint)
    }
}
