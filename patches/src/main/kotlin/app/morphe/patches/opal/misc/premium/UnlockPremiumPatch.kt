/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.opal.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatLifetimeEntitlement
import app.morphe.patches.shared.misc.smali.parameterRegister

private const val ENTITLEMENT = "premium_tier_1"
private const val PRODUCT = "premium_tier_1_lifetime"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Opal Pro.",
) {
    compatibleWith(AppCompatibilities.OPAL)
    dependsOn(removePairipProtectionPatch)
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatLifetimeEntitlement(ENTITLEMENT, PRODUCT)

        BlockScreenCategoryConstructorFingerprint.matchSingle().method.apply {
            val isProParameterIndex = parameterTypes.indexOfFirst { it == "Z" }
            if (isProParameterIndex < 0) throw PatchException("BlockScreenCategoryUiModel has no boolean parameter")
            addInstructions(0, "const/4 ${parameterRegister(isProParameterIndex)}, 0x0")
        }
    }
}
