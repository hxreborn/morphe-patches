/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.symfonium.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks all premium features.",
) {
    compatibleWith(AppCompatibilities.SYMFONIUM)

    execute {
        val (licensedStateLiteral) = IsLicensedFingerprint.match().instructionMatches
        val licensedState = licensedStateLiteral.getInstruction<WideLiteralInstruction>()
            .wideLiteral

        LicenseStateWriterFingerprint.method
            .addInstructions(0, "const-wide p1, ${licensedState}L")

        LicenseKeyCheckFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        TrialExpiryTextFingerprint.method.returnEarly(null)

        NativeVerdictHandlerFingerprint.method.returnEarly()
    }
}
