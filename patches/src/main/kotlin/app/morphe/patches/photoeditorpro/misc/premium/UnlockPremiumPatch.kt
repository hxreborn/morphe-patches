/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.premium

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.photoeditorpro.misc.fix.platform.spoofIosPlatformPatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.ProGateFingerprint
import app.morphe.patches.photoeditorpro.shared.RemoveAdsPurchasedFingerprint
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.markPatchMetadata
import app.morphe.patches.photoeditorpro.shared.returnTrueWhileEnabled
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks pro tools and removes export watermarks and upgrade prompts.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofIosPlatformPatch, spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        markPatchMetadata("unlockPremium", unlockPremiumPatch)
        val adsRemoved = RemoveAdsPurchasedFingerprint.matchSingle().method.name

        ProGateFingerprint.matchAll(2..2)
            .map { it.method }
            .filter { it.name != adsRemoved }
            .singleOrNull()
            ?.returnTrueWhileEnabled("unlockPremium")
            ?: throw PatchException("Expected exactly one pro-gate method distinct from $adsRemoved")
        markPatchInstalled("pep_unlock_premium")
    }
}
