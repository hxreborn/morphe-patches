/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes splash, chat list, story and end-of-call ads, and skips the ad consent form.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        ShouldShowAdFingerprint.matchAll().forEach { it.method.returnEarly(false) }
        ConsentSdkAvailabilityFingerprint.matchSingle().method.returnEarly(false)
    }
}
