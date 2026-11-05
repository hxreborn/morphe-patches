/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied

private const val APPLICATION_CLASS = "Lcom/viber/voip/ManifestViberApplication;"

private const val DESCRIPTION = "Lets the app send and receive messages and get its push token."

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.VIBER)

    extendWith("extensions/extension.mpe")

    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("spoofSignature", DESCRIPTION)
        spoofSignature(APPLICATION_CLASS)
    }
}
