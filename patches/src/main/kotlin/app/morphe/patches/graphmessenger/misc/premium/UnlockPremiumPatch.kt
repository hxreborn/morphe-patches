/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.graphmessenger.shared.licenseDaysLeft
import app.morphe.patches.graphmessenger.shared.licenseExpired
import app.morphe.patches.graphmessenger.shared.overrideResultsOf
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

private const val LICENSE_DAYS_LEFT = 999

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Graph Premium and Telegram Premium features in the client. " +
        "Features Telegram's servers meter still need a Premium account.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        UserIsPremiumFingerprint.matchSingle().method.returnEarly(true)
        LicenseDaysLeftFingerprint.matchAll().forEach { it.method.overrideResultsOf(licenseDaysLeft, LICENSE_DAYS_LEFT) }
        LicenseExpiredFingerprint.matchAll().forEach { it.method.overrideResultsOf(licenseExpired, 0) }
    }
}
