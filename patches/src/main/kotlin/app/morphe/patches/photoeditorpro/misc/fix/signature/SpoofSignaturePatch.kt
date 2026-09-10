/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.shared.PATCH_APPLICATION_CLASS
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(PAIRIP_APPLICATION_CLASS, hostClass = PATCH_APPLICATION_CLASS)

        InitializeLicenseCheckFingerprint.matchSingle().method.returnEarly()

        markPatchInstalled("pep_spoof_signature")
    }
}
