/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.liftoff.misc.premium

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.liftoff.misc.settings.LIFTOFF_PATCH_SETTINGS_CLASS
import app.morphe.patches.liftoff.misc.settings.markPatchApplied
import app.morphe.patches.liftoff.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

private const val ENTITLEMENT = "gymbros_plus_v1"
private const val PRODUCT = "gymbros_plus_v1"
@Suppress("unused")
val unlockPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks rank analysis, bodygraph, full chart history and nutrition tools. " +
        "Requires a Liftoff account.",
) {
    dependsOn(patchesSettingsPatch)
    compatibleWith(AppCompatibilities.LIFTOFF)
    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("unlockPremium", unlockPremiumPatch)
        grantRevenueCatEntitlements(
            ENTITLEMENT to PRODUCT,
            enabledMethod = "$LIFTOFF_PATCH_SETTINGS_CLASS->isPremiumUnlocked()Z",
        )
    }
}
