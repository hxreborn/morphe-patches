/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gsmarena.ads

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes banner and interstitial ads and the sponsor image in the side menu.",
) {
    compatibleWith(AppCompatibilities.GSMARENA)

    execute {
        CanLoadAdsFingerprint.method.returnEarly(false)
        SponsorImageFingerprint.method.returnEarly()
    }
}
