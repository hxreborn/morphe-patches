/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.apnatunnel.misc.ads

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.apnatunnel.misc.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.ConsentInfoUpdateFingerprint

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes app open, banner and interstitial ads and the ad consent form.",
) {
    compatibleWith(AppCompatibilities.APNA_TUNNEL_LITE)

    dependsOn(spoofSignaturePatch)

    execute {
        AdLoadFingerprint.method.returnEarly()
        ConsentInfoUpdateFingerprint.method.returnEarly()
    }
}
