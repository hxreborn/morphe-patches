/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val removePairipProtectionPatch = bytecodePatch(
    name = "Remove pairip protection",
    description = "Removes the Play Integrity license check.",
) {
    compatibleWith(
        AppCompatibilities.RATEGLANCE,
    )

    execute {
        CheckLicenseFingerprint.matchSingle().method.returnEarly()
    }
}
