/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.ProGateFingerprint
import app.morphe.patches.photoeditorpro.shared.RemoveAdsPurchasedFingerprint
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.returnTrueWhileEnabled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Adds an option to unlock the pro tools, remove the export watermark and " +
        "hide the upgrade prompts.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
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
