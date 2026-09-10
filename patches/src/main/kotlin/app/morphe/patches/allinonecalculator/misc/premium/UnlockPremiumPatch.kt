/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.BuildCustomerInfoFingerprint
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/RevenueCatUnlock;"
private const val ENTITLEMENT = "pro"
private const val PRODUCT = "pro_version"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Grants the pro entitlement, which removes the ads and the paywalled tools.",
) {
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)
    extendWith("extensions/extension.mpe")

    dependsOn(removePairipVirtualizationPatch)

    execute {
        BuildCustomerInfoFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 3)
            val jsonRegister = registers.getFreeRegister4Bit()
            val entitlementRegister = registers.getFreeRegister4Bit()
            val productRegister = registers.getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    move-object/from16 v$jsonRegister, p1
                    const-string v$entitlementRegister, "$ENTITLEMENT"
                    const-string v$productRegister, "$PRODUCT"
                    invoke-static { v$jsonRegister, v$entitlementRegister, v$productRegister }, $EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                """,
            )
        }
    }
}
