/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.gms

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import app.morphe.patches.shared.misc.gms.gmsCoreManifestPatch
import app.morphe.patches.shared.misc.gms.gmsCoreSpoofedSignaturePatch
import app.morphe.patches.shared.misc.gms.renameGmsReferencesToGmsCore
import app.morphe.patches.shared.misc.requireStandardInstall

@Suppress("unused")
val gmsCoreSupportPatch: Patch = patch(
    name = "GmsCore support",
    description = "Lets you add Google accounts through GmsCore. Requires GmsCore.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    availability(requireStandardInstall)

    dependsOn(fixSignInPatch, gmsCoreSpoofedSignaturePatch, gmsCoreManifestPatch)

    execute {
        markPatchApplied("gmsCoreSupport", gmsCoreSupportPatch)
        renameGmsReferencesToGmsCore()
    }
}
