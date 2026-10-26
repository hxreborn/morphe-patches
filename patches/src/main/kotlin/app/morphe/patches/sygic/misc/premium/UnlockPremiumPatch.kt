/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.sygic.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val PREMIUM_VALID_UNTIL_MILLIS = "4102444800000"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium+ features, including live traffic, speed cameras and " +
        "Motorbike mode.",
) {
    compatibleWith(AppCompatibilities.SYGIC)

    execute {
        FeatureIsValidFingerprint.matchSingle().method.returnEarly(true)
        PremiumLicenseConstructorFingerprint.matchSingle()

        LicenseParserFingerprint.matchSingle().let { match ->
            val constructorIndex = match.instructionMatches.last().index
            match.method.apply {
                val constructor = getInstruction<FiveRegisterInstruction>(constructorIndex)
                val licenseRegister = constructor.registerD
                val registers = getFreeRegisterProvider(
                    constructorIndex,
                    2,
                    constructor.registerC,
                    licenseRegister,
                    constructor.registerE,
                )
                val premiumPlusRegister = registers.getFreeRegister4Bit()
                val validUntilRegister = registers.getFreeRegister4Bit()

                addInstructions(
                    constructorIndex,
                    """
                        const/4 v$premiumPlusRegister, 0x1
                        const-string v$validUntilRegister, "$PREMIUM_VALID_UNTIL_MILLIS"
                        invoke-static { v$validUntilRegister }, Ljava/lang/Long;->valueOf(Ljava/lang/String;)Ljava/lang/Long;
                        move-result-object v$validUntilRegister
                        new-instance v$licenseRegister, $PREMIUM_LICENSE_CLASS
                        invoke-direct { v$licenseRegister, v$premiumPlusRegister, v$validUntilRegister }, $PREMIUM_LICENSE_CLASS-><init>(ZLjava/lang/Long;)V
                    """,
                )
            }
        }
    }
}
