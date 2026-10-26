/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.expo.disableOtaUpdatesPatch
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val IS_PRO_FROM_USER_1_4 =
    "90 09 04 37 02 03 01 2A 6D 0B 02 02 0B 02 02 40 00 02 2A 6D"
private const val IS_PRO_TRUE_1_4 =
    "90 09 04 37 02 03 01 2A 6D 78 02 78 02 78 02 40 00 02 2A 6D"

private const val SYNC_INTERVAL_FREE_1_4 =
    "37 05 05 07 4B B7 90 0B 06 37 06 05 08 18 57 8E 08 37 06 05 09 A1 55"
private const val SYNC_INTERVAL_PRO_1_4 =
    "37 05 05 07 4B B7 90 0B 06 37 06 05 08 A1 55 8E 08 37 06 05 09 A1 55"
private const val IS_PRO_FROM_USER_1_5 =
    "90 09 04 37 02 03 01 9D 6D 0B 02 02 0B 02 02 40 00 02 9D 6D"
private const val IS_PRO_TRUE_1_5 =
    "90 09 04 37 02 03 01 9D 6D 78 02 78 02 78 02 40 00 02 9D 6D"

private const val SYNC_INTERVAL_FREE_1_5 =
    "37 05 05 07 43 B8 90 0B 06 37 06 05 08 93 57 8E 08 37 06 05 09 2B 56"
private const val SYNC_INTERVAL_PRO_1_5 =
    "37 05 05 07 43 B8 90 0B 06 37 06 05 08 2B 56 8E 08 37 06 05 09 2B 56"

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks unlimited move views, goals and move cards, manual sync, earlier " +
        "library updates and card customization. Disables OTA updates.",
) {
    compatibleWith(AppCompatibilities.MYMOVESET)

    dependsOn(
        disableOtaUpdatesPatch,
        removePairipProtectionPatch,
        hermesVariantsPatch(
            setOf(
                IS_PRO_FROM_USER_1_4 to IS_PRO_TRUE_1_4,
                SYNC_INTERVAL_FREE_1_4 to SYNC_INTERVAL_PRO_1_4,
            ),
            setOf(
                IS_PRO_FROM_USER_1_5 to IS_PRO_TRUE_1_5,
                SYNC_INTERVAL_FREE_1_5 to SYNC_INTERVAL_PRO_1_5,
            ),
        ),
    )
}
