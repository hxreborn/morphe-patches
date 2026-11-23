/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.echoequalizer.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val ENTITLEMENT = "echoeq Pro"
private const val PRODUCT = "echo-pro-1"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Echo Pro, including the 15- and 31-band equalizers, compressor, limiter and Safe Hearing protect mode.",
) {
    dependsOn(sharedRevenuecatExtensionPatch, sharedSignatureExtensionPatch)
    dependsOn(removePairipProtectionPatch)
    compatibleWith(AppCompatibilities.ECHO_EQUALIZER)

    execute {
        spoofSignature(APPLICATION_CLASS)

        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}
