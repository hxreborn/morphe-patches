/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch

private const val ENTITLEMENT = "basic"
private const val PRODUCT = "basic"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks MuscleWiki Premium and removes ads. " +
        "Requires a signed-in account.",
) {
    dependsOn(sharedRevenuecatExtensionPatch)
    compatibleWith(AppCompatibilities.MUSCLEWIKI)
    availability(requireArm64)
    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
        val library = dartAotLibrary()
        val bytes = library.readBytes()
        bytes.forceProfilePremium()
        library.writeBytes(bytes)
    }
}
