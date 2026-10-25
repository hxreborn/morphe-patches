/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides ads in the chat list, the More screen, Explore, search and after calls.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    execute {
        AdsEnabledFingerprint.matchSingle().method.returnEarly(false)
    }
}
