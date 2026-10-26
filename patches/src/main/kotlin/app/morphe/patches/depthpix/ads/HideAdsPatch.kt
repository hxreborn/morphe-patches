/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes banner, native, interstitial and rewarded ads.",
) {
    compatibleWith(AppCompatibilities.DEPTHPIX)

    dependsOn(removePairipProtectionPatch)

    execute {
        AppAdConfigConstructorFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x0")
    }
}
