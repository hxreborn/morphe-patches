/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes fullscreen ads when opening projects.",
) {
    compatibleWith(AppCompatibilities.FLIPACLIP)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
    availability(requireArm64)

    execute {
        val match = ShowInterstitialFingerprint.matchSingle()
        val branchIndex = match.instructionMatches.single { it.filter === interstitialStateBranch }.index
        match.method.replaceInstruction(branchIndex, "nop")
    }
}
