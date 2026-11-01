/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tunneldoom.misc.time

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val removeTimeLimitPatch = bytecodePatch(
    name = "Remove time limit",
    description = "Keeps the VPN connected without adding time by watching ads.",
) {
    compatibleWith(AppCompatibilities.TUNNELDOOM_VPN)

    dependsOn(removePairipProtectionPatch)

    execute {
        CountdownStartFingerprint.matchSingle().method.addInstruction(0, "return-object p0")
    }
}
