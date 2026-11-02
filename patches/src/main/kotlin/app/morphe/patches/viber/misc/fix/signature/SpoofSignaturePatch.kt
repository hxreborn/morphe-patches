/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature

private const val APPLICATION_CLASS = "Lcom/viber/voip/ManifestViberApplication;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Lets the app send and receive messages and get its push token.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
