/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.gms

import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.raindrop.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreManifestPatch
import app.morphe.patches.shared.misc.gms.gmsCoreSpoofedSignaturePatch
import app.morphe.patches.shared.misc.gms.renameGmsReferencesToGmsCore

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in with Google through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
    default = false,
) {
    compatibleWith(AppCompatibilities.RAINDROP)

    availability { installer, _ ->
        when (installer) {
            InstallerType.MOUNT -> PatchAvailability.UNAVAILABLE
            else -> PatchAvailability.DISABLED
        }
    }

    dependsOn(spoofSignaturePatch, gmsCoreSpoofedSignaturePatch, gmsCoreManifestPatch)

    execute {
        renameGmsReferencesToGmsCore()
    }
}
