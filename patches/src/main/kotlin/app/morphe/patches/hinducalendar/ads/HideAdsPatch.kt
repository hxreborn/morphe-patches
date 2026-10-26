/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hinducalendar.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.hinducalendar.misc.pairip.restoreReminderReceiversPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes banner and interstitial ads and the Remove Ads menu item.",
) {
    compatibleWith(AppCompatibilities.HINDU_CALENDAR)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreReminderReceiversPatch)

    execute {
        val match = OnQueryPurchasesResponseFingerprint.matchSingle()
        val isEmptyResult = match.instructionMatches[2]
        val register = isEmptyResult.getInstruction<OneRegisterInstruction>().registerA

        match.method.addInstruction(isEmptyResult.index + 1, "const/4 v$register, 0x0")
    }
}
