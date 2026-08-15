/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.symfonium.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all premium features.",
) {
    compatibleWith(AppCompatibilities.SYMFONIUM)

    execute {
        val keyCheck = LicenseKeyCheckFingerprint.matchSingle()
        val licenseManager = keyCheck.classDef

        val isLicensed = licenseManager.methods.single {
            it.returnType == "Z" && it.parameters.isEmpty()
        }
        val licensedState = isLicensed.implementation!!.instructions
            .take(isLicensed.indexOfFirstInstructionOrThrow(Opcode.CMP_LONG))
            .filterIsInstance<WideLiteralInstruction>()
            .last()
            .wideLiteral

        licenseManager.methods
            .single { it.returnType == "V" && it.parameters.singleOrNull()?.type == "J" }
            .addInstructions(0, "const-wide p1, ${licensedState}L")

        keyCheck.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        NativeVerdictHandlerFingerprint.matchSingle().method.returnEarly()
    }
}
