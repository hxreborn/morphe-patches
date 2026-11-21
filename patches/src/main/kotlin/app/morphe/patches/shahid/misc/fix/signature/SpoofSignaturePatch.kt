/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.fix.signature

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lnet/mbc/shahid/ShahidApplication;"

val spoofSignaturePatch = patch {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.SHAHID)
    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
