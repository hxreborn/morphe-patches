/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.RemoveAdsPurchasedFingerprint
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.returnTrueWhileEnabled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Adds an option to hide banner, interstitial, app-open and rewarded ads.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        RemoveAdsPurchasedFingerprint.matchSingle().method.returnTrueWhileEnabled("hideAds")
        markPatchInstalled("pep_hide_ads")
    }
}
