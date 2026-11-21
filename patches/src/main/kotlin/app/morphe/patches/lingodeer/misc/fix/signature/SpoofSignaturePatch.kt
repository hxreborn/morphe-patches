/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lingodeer.misc.pairip.restoreLifecyclePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lcom/pairip/application/Application;"

val spoofSignaturePatch = bytecodePatch {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.LINGODEER)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreLifecyclePatch)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
