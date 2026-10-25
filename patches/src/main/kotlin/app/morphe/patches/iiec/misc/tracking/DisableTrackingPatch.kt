/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iiec.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.iiec.misc.fix.signature.bypassSignatureCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics from collecting usage data.",
) {
    compatibleWith(*AppCompatibilities.IIEC_APPS)

    dependsOn(bypassSignatureCheckPatch)

    execute {
        EnableFirebaseFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x0")
    }
}
