/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.restrictions.filterReturnValue
import app.morphe.patches.protonvpn.misc.restrictions.freeAccountStatePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.matchSingle

private const val FREE_SERVER_LOCATIONS = "Lapp/hxreborn/extension/protonvpn/FreeServerLocations;"

@Suppress("unused")
val unlockProfilesPatch = bytecodePatch(
    name = "Unlock profiles",
    description = "Unlocks profiles on free plans and limits them to free locations.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, freeAccountStatePatch)

    execute {
        markPatchApplied("unlockProfiles")
        clearFreeUserCheck(ProfileAvailabilityFingerprint)
        VpnCountriesFingerprint.matchSingle().method.filterReturnValue(
            "$FREE_SERVER_LOCATIONS->countriesForAccount(Ljava/util/List;)Ljava/util/List;",
        )
        profileTypesFingerprints.forEach { fingerprint ->
            fingerprint.matchSingle().method.filterReturnValue(
                "$FREE_SERVER_LOCATIONS->profileTypesForAccount(Ljava/util/List;)Ljava/util/List;",
            )
        }
    }
}
