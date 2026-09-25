/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hinducalendar.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes banner and interstitial ads and the Remove Ads menu item.",
) {
    compatibleWith(AppCompatibilities.HINDU_CALENDAR)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        val match = OnQueryPurchasesResponseFingerprint.matchSingle()
        val isEmptyResult = match.instructionMatches[2]
        val register = isEmptyResult.getInstruction<OneRegisterInstruction>().registerA
        val adFreeField = match.instructionMatches.last()
            .getInstruction<ReferenceInstruction>()
            .getReference<FieldReference>()!!

        match.method.addInstruction(isEmptyResult.index + 1, "const/4 v$register, 0x0")

        val constructor = MainActivityConstructorFingerprint.matchSingle()
        constructor.method.addInstructions(
            constructor.instructionMatches.last().index,
            """
                const/4 v0, 0x1
                iput-boolean v0, p0, $adFreeField
            """,
        )
    }
}
