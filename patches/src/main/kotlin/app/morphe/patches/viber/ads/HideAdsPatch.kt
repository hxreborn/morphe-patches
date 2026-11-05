/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled

private const val DESCRIPTION = "Hides ads in the chat list, the More screen, Explore, search and after calls."

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.VIBER)

    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("hideAds", DESCRIPTION)
        AdsEnabledFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->hideAds()Z")
    }
}
