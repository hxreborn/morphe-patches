/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ledblinker.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val LIFETIME_LICENSE = 1

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks pocket mode, notification history and statistics, and removes ads.",
) {
    dependsOn(removePairipProtectionPatch)
    compatibleWith(AppCompatibilities.LED_BLINKER)

    execute {
        LifetimeLicenseStateFingerprint.matchSingle().method.returnEarly(LIFETIME_LICENSE)
        SetupFullVersionFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x1")
    }
}
