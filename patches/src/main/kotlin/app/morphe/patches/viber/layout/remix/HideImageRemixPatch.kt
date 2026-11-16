/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.remix

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch

@Suppress("unused")
val hideImageRemixPatch = bytecodePatch(
    name = "Hide image remix",
    description = "Hides Remix actions and the Remix introduction popup on images.",
    default = false,
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch)

    execute {
        ImageRemixAvailabilityFingerprint.matchSingle().method.returnEarly(false)
    }
}
