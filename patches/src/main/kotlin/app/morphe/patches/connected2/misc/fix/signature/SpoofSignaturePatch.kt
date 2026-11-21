/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.fix.signature

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lcom/pairip/application/Application;"

@Suppress("unused")
val spoofSignaturePatch = patch(
    name = "Spoof signature",
    description = "Enables sign-up and sign-in.",
) {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
