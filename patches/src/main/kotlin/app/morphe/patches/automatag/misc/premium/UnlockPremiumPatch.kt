/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.automatag.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.automatag.misc.signature.skipLicenseCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks AutomaTag Pro, including batch modes, audio fingerprinting " +
        "and duplicate detection, and removes ads.",
) {
    compatibleWith(AppCompatibilities.AUTOMATAG)

    dependsOn(skipLicenseCheckPatch)

    execute {
        IsProductPurchasedFingerprint.matchSingle().method.returnEarly(true)
        IsBillingProductOwnedFingerprint.matchSingle().method.returnEarly(true)
    }
}
