/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.upselling

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.all.misc.hermes.hermesPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val ONBOARDING_PUSHES_PAYWALL =
    "73 05 58 0B 40 01 05 A0 5B 03 05 73 06 57 0B"
private const val ONBOARDING_PUSHES_TABS =
    "73 05 C8 08 40 01 05 A0 5B 03 05 73 06 57 0B"

@Suppress("unused")
val hideUpgradePromotionsPatch = rawResourcePatch(
    name = "Hide upgrade promotions",
    description = "Skips the subscription offer shown after sign-up.",
) {
    compatibleWith(AppCompatibilities.STEEZY)

    dependsOn(
        hermesPatch {
            setOf(ONBOARDING_PUSHES_PAYWALL to ONBOARDING_PUSHES_TABS)
        },
    )
}
