/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lingodeer.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks every course, lesson and practice mode.",
) {
    compatibleWith(AppCompatibilities.LINGODEER)

    availability(requireArm)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, spoofSignaturePatch)

    execute {
        val match = PremiumStateFingerprint.matchSingle()
        val isEmptyResultIndex = match.instructionMatches.last().index + 1
        val register = match.method.getInstruction<OneRegisterInstruction>(isEmptyResultIndex).registerA
        match.method.replaceInstruction(isEmptyResultIndex, "const/4 v$register, 0x0")
    }
}
