/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.parameterRegister
import app.morphe.patches.yuka.misc.pairip.guardVirtualizedTasksPatch

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks food preferences, diet alerts and the full history. " +
        "Search and offline downloads don't work. " +
        "Signing in needs Sign-in token, except on a root mount install.",
) {
    compatibleWith(AppCompatibilities.YUKA)

    availability(requireArm64)

    dependsOn(guardVirtualizedTasksPatch, removePairipProtectionPatch)

    execute {
        YukaUserConstructorFingerprint.matchSingle().method.apply {
            addInstructions(0, "const/16 ${parameterRegister(parameterTypes.lastIndex)}, 0x1")
        }

        CachedPremiumFingerprint.matchSingle().method.returnEarly(true)
    }
}
