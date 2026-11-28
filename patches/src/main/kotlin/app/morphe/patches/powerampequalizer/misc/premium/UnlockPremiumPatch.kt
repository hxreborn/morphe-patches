/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.powerampequalizer.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.powerampequalizer.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
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

        val (_, titleSetter) = LicenseStatusTitleFingerprint.match().instructionMatches
        val titleSetterCall = titleSetter.getInstruction<FiveRegisterInstruction>()
        val preference = titleSetterCall.registerC
        val title = titleSetterCall.registerD
        LicenseStatusTitleFingerprint.method.apply {
            val registers = getFreeRegisterProvider(titleSetter.index, 2, preference, title)
            val application = registers.getFreeRegister4Bit()
            val editionId = registers.getFreeRegister4Bit()
            addInstructions(
                titleSetter.index,
                """
                    invoke-virtual {v$preference}, Landroid/preference/Preference;->getContext()Landroid/content/Context;
                    move-result-object v$application
                    invoke-virtual {v$application}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
                    move-result-object v$application
                    check-cast v$application, Lcom/maxmpz/app/base/BaseApplication;
                    const v$editionId, ${resourceId(ResourceType.ID, "state_app_edition_name")}
                    invoke-virtual {v$application, v$editionId}, Lcom/maxmpz/app/base/BaseApplication;->getStringState(I)Ljava/lang/String;
                    move-result-object v$title
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
