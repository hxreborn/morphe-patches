/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.allinonecalculator.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/allinonecalculator/PremiumUnlock;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Grants the pro entitlement, which removes the ads and the paywalled tools.",
) {
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)
    extendWith("extensions/extension.mpe")

    dependsOn(removePairipProtectionPatch)

    execute {
        BuildCustomerInfoFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;)V",
        )
    }
}
