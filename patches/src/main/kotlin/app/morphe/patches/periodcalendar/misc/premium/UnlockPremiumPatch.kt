/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.periodcalendar.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Premium and the cycle synchronization workbook. Removes ads.",
) {
    compatibleWith(AppCompatibilities.PERIOD_CALENDAR)

    execute {
        IsProFingerprint.method.returnEarly(true)
        IsAdFreeFingerprint.method.returnEarly(true)

        val workbook = WorkbookPurchasedFingerprint.match()
        val (_, ownershipResult) = workbook.instructionMatches
        val ownershipCheckIndex = ownershipResult.index
        val ownedRegister = workbook.method.getInstruction<OneRegisterInstruction>(ownershipCheckIndex).registerA
        workbook.method.addInstruction(ownershipCheckIndex + 1, "const/16 v$ownedRegister, 0x1")
    }
}
