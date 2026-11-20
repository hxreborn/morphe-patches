/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.calcnote.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lcom/burton999/notecal/CalcNoteApplication;"

val spoofSignaturePatch = bytecodePatch {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.CALCNOTE)
    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
