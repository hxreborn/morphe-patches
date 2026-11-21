/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removePremiumEntryPatch = patch(
    name = "Remove premium entry",
    description = "Removes the imo Premium entry from the Me page.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        MePagePremiumEntryFingerprint.matchSingle().method.returnEarly(false)
    }
}
