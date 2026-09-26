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

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val removeServerChangeDelayPatch = bytecodePatch(
    name = "Remove server change delay",
    description = "Removes the wait between server changes on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("removeServerChangeDelay")
        changeServerDelayFingerprints.forEach { it.matchSingle().method.returnEarly(0) }
    }
}
