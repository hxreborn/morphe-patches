/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.apnatunnel.misc.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.apnatunnel.misc.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes app open, banner and interstitial ads and the ad consent form.",
) {
    compatibleWith(AppCompatibilities.APNA_TUNNEL_LITE)

    dependsOn(spoofSignaturePatch)

    execute {
        AdLoadFingerprint.matchSingle().method.returnEarly()
        ConsentInfoUpdateFingerprint.matchSingle().method.returnEarly()
    }
}
