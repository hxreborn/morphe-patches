/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.powerampequalizer.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.powerampequalizer.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val VERIFIED_FULL_VERSION_LICENSE = 272

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium features and removes the time-limited trial.",
) {
    compatibleWith(AppCompatibilities.POWERAMP_EQUALIZER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        val (_, licenseStateRead, licenseValueRead) = LicenseStateReadFingerprint.match().instructionMatches
        val licenseState = licenseStateRead.instruction.getReference<FieldReference>()!!
        val licenseValue = licenseValueRead.instruction.getReference<FieldReference>()!!

        val licenseStateInit = licenseStateInitFingerprint(licenseState).match()
        val (_, initReturn) = licenseStateInit.instructionMatches
        licenseStateInit.method.apply {
            val registers = getFreeRegisterProvider(initReturn.index, 2)
            val licenseHolder = registers.getFreeRegister4Bit()
            val license = registers.getFreeRegister4Bit()
            addInstructions(
                initReturn.index,
                """
                    sget-object v$licenseHolder, $licenseState
                    const/16 v$license, $VERIFIED_FULL_VERSION_LICENSE
                    iput v$license, v$licenseHolder, $licenseValue
                """,
            )
        }

        val (_, _, licenseResultRead) = LicenseResultFingerprint.match().instructionMatches
        val resultRegister = licenseResultRead.getInstruction<OneRegisterInstruction>().registerA
        LicenseResultFingerprint.method.addInstruction(
            licenseResultRead.index + 1,
            "const/16 v$resultRegister, $VERIFIED_FULL_VERSION_LICENSE",
        )
    }
}
