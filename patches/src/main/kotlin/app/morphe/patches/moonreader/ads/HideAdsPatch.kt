/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moonreader.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides full-screen ads and the Remove ads button in the side menu.",
) {
    compatibleWith(AppCompatibilities.MOON_READER)

    execute {
        DisableAdsFingerprint.matchSingle().method.returnEarly(true)
        CanShowAdsFingerprint.matchSingle().method.returnEarly(false)
    }
}
