/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.terabox.misc.fix.integrity.disablePlayIntegrityTokensPatch

private const val APPLICATION_CLASS = "Lcom/dubox/drive/shell/ShellApplication;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(disablePlayIntegrityTokensPatch)
    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
