/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hourlychime.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val PREMIUM_DECISION_COUNT = 5
private const val AD_ELIGIBILITY_COUNT = 3

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium chime settings and removes ads.",
) {
    compatibleWith(AppCompatibilities.HOURLY_CHIME)
    dependsOn(removePairipProtectionPatch)

    execute {
        LicenseStateFingerprint.method.addInstructions(
            0,
            "sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;\nreturn-object v0",
        )
        PremiumDecisionFingerprint.matchAll(PREMIUM_DECISION_COUNT..PREMIUM_DECISION_COUNT).forEach { it.method.returnEarly(true) }

        AdEligibilityFingerprint.matchAll(AD_ELIGIBILITY_COUNT..AD_ELIGIBILITY_COUNT).forEach { it.method.returnEarly(false) }
        HomeAdRequestFingerprint.method.returnEarly(null)
        HomeAdRenderFingerprint.method.returnEarly()
        DelayedHomeAdFingerprint.method.returnEarly()
    }
}
