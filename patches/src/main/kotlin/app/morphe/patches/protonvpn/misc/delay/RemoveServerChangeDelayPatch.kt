/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/delay/RemoveDelayPatch.kt
 */
package app.morphe.patches.protonvpn.misc.delay

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.VPN_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.markPatchApplied

@Suppress("unused")
val removeServerChangeDelayPatch: Patch = patch(
    name = "Remove server change delay",
    description = "Removes the wait between server changes on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN, AppCompatibilities.PROTON_VPN_FDROID)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied(VPN_SETTINGS_CLASS, "removeServerChangeDelay", removeServerChangeDelayPatch)
        changeServerDelayFingerprints.forEach { it.method.returnEarly(0) }
    }
}
