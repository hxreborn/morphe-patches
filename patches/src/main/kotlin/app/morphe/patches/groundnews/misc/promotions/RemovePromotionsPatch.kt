/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.promotions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removePromotionsPatch = bytecodePatch(
    name = "Remove promotions",
    description = "Removes subscription promotions from the main screen.",
) {
    compatibleWith(AppCompatibilities.GROUND_NEWS)

    execute {
        SubscriptionBannerVisibilityFingerprint.matchSingle().method.returnEarly(false)
    }
}
