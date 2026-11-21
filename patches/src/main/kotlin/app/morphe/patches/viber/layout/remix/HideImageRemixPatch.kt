/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.remix

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled

private const val HIDE_IMAGE_REMIX = "$VIBER_SETTINGS_CLASS->hideImageRemix()Z"

@Suppress("unused")
val hideImageRemixPatch: Patch = patch(
    name = "Hide image remix",
    description = "Hides Remix actions and the Remix introduction popup on images.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("hideImageRemix", hideImageRemixPatch)
        ImageRemixAvailabilityFingerprint.matchSingle().method.returnWhenEnabled(HIDE_IMAGE_REMIX)
    }
}
