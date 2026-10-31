/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes ads and the advertising consent prompt.",
) {
    compatibleWith(AppCompatibilities.PIXLR)

    dependsOn(adRemovalEntitlementPatch)

    execute {
        RequestAdConsentFingerprint.matchSingle().method.apply {
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val completionIndex = indexOfFirstInstructionReversedOrThrow(returnIndex, Opcode.SGET_OBJECT)

            addInstructionsWithLabels(
                0,
                "goto :completion",
                ExternalLabel("completion", getInstruction(completionIndex)),
            )
        }
    }
}
