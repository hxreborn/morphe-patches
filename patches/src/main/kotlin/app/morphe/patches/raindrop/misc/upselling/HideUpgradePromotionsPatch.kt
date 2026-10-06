/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.upselling

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.all.misc.hermes.hermesPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val GO_PRO_ITEM =
    "34 05 00 41 04 01 00 00 00 89 01 01 44 06 01 00 A0 44 01 01 01 B6 37 04 00 01 3B 01 05 04 " +
        "44 03 01 02 9A 3B 01 05 03 44 02 01 03 68 01 01 3A 08 83 CA 52 01 06 00 3B 06 05 02 44 08 " +
        "06 03 68 44 07 08 04 0E 90 06 72 87"
private const val GO_PRO_ITEM_RETURNS_NULL =
    "94 01 76 01 04 01 00 00 00 89 01 01 44 06 01 00 A0 44 01 01 01 B6 37 04 00 01 3B 01 05 04 " +
        "44 03 01 02 9A 3B 01 05 03 44 02 01 03 68 01 01 3A 08 83 CA 52 01 06 00 3B 06 05 02 44 08 " +
        "06 03 68 44 07 08 04 0E 90 06 72 87"

@Suppress("unused")
val hideUpgradePromotionsPatch = rawResourcePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Go Pro entry in Settings.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)

    dependsOn(
        hermesPatch {
            setOf(GO_PRO_ITEM to GO_PRO_ITEM_RETURNS_NULL)
        },
    )
}
