/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.upselling

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch

private const val GO_PRO_ITEM =
    "34 03 00 41 02 01 00 00 00 89 06 01 3B 0B 03 00 3B 05 03 01 5E 04 05 04 93 01 6E 04 0B 01 04 " +
        "44 04 04 00 1B 8B 00 09 6E 04 04 01 00 44 07 06 01 A0"
private const val GO_PRO_ITEM_RETURNS_NULL =
    "94 01 76 01 02 01 00 00 00 89 06 01 3B 0B 03 00 3B 05 03 01 5E 04 05 04 93 01 6E 04 0B 01 04 " +
        "44 04 04 00 1B 8B 00 09 6E 04 04 01 00 44 07 06 01 A0"

@Suppress("unused")
val hideUpgradePromotionsPatch = rawResourcePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Go Pro entry in Settings.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)

    dependsOn(hermesVariantsPatch(setOf(GO_PRO_ITEM to GO_PRO_ITEM_RETURNS_NULL)))
}
