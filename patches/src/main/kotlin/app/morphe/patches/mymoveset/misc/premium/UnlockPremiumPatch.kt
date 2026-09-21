/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.all.misc.hermes.hermesPatch
import app.morphe.patches.mymoveset.misc.updates.disableOtaUpdatesPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val IS_PRO_FROM_USER =
    "90 09 04 37 02 03 01 80 56 0B 02 02 0B 02 02 40 00 02 80 56"
private const val IS_PRO_TRUE =
    "90 09 04 37 02 03 01 80 56 78 02 78 02 78 02 40 00 02 80 56"

private const val SYNC_INTERVAL_FREE =
    "37 05 05 07 9B 9D 90 0B 06 37 06 05 08 E8 41 8E 08 37 06 05 09 F6 40"
private const val SYNC_INTERVAL_PRO =
    "37 05 05 07 9B 9D 90 0B 06 37 06 05 08 F6 40 8E 08 37 06 05 09 F6 40"

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks unlimited move views, goals and move cards, manual sync, earlier " +
        "library updates and card customization. Disables OTA updates.",
) {
    compatibleWith(AppCompatibilities.MYMOVESET)

    dependsOn(
        disableOtaUpdatesPatch,
        hermesPatch {
            setOf(
                IS_PRO_FROM_USER to IS_PRO_TRUE,
                SYNC_INTERVAL_FREE to SYNC_INTERVAL_PRO,
            )
        },
    )
}
