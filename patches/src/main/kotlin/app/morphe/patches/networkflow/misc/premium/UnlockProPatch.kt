/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.networkflow.misc.fix.signature.disableArchiveSignatureCheckPatch
import app.morphe.patches.networkflow.misc.fix.signature.disableNativeChecksPatch
import app.morphe.patches.networkflow.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val unlockProPatch = patch(
    name = "Unlock pro",
    description = "Unlocks the full app ranking, all usage periods and date ranges, the Total Speed " +
        "notification layout, colourful Home cards and unlimited data plans. Removes ads. Requires an arm64 device.",
) {
    compatibleWith(AppCompatibilities.NETWORKFLOW)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch, disableArchiveSignatureCheckPatch, disableNativeChecksPatch)

    execute {
        ProStatusDefaultConstructorFingerprint.matchSingle().method.addInstructions(0, "const/4 p1, 0x0")
    }
}
