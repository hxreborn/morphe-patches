/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.banglacalendar.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val BOOLEAN_TRUE = "Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;"
private const val BOOLEAN_FALSE = "Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium and removes ads.",
) {
    compatibleWith(AppCompatibilities.BANGLA_CALENDAR)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        PremiumCodeFingerprint.matchSingle().method.returnEarly("p")

        BillingStateConstructorFingerprint.matchSingle().let { match ->
            val factoryCall = match.instructionMatches.first()
            val flag = factoryCall.getInstruction<FiveRegisterInstruction>().registerC
            match.method.addInstruction(factoryCall.index + 2, "sget-object v$flag, $BOOLEAN_FALSE")
            match.method.addInstruction(factoryCall.index, "sget-object v$flag, $BOOLEAN_TRUE")
        }

        PurchasesQueriedFingerprint.matchSingle().let { match ->
            val boxing = match.instructionMatches.first()
            val flag = boxing.getInstruction<FiveRegisterInstruction>().registerC
            match.method.addInstruction(boxing.index, "const/4 v$flag, 0x1")
        }
    }
}
