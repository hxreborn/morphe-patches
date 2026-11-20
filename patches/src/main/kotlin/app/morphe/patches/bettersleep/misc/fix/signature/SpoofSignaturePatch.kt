/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lipnossoft/rma/free/RelaxMelodiesAppFree;"

val spoofSignaturePatch = bytecodePatch {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.BETTERSLEEP)
    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
