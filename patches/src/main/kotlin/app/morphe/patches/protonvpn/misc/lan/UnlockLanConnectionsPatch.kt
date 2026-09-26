/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/lan/UnlockLanConnectionsPatch.kt
 */
package app.morphe.patches.protonvpn.misc.lan

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.unlockUserSetting
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied

@Suppress("unused")
val unlockLanConnectionsPatch = bytecodePatch(
    name = "Unlock LAN connections",
    description = "Unlocks LAN connections on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockLanConnections")
        unlockUserSetting(
            LanConnectionsViewStateFingerprint,
            freeUserParameter = 3,
            LanConnectionsRestrictionFingerprint,
        )
    }
}
