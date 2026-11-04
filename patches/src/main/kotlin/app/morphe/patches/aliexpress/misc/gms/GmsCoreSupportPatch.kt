/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.gms

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor

private val gmsCoreSupportMarkerPatch = bytecodePatch {
    dependsOn(spoofSignaturePatch)

    execute {
        markPatchApplied("gmsCoreSupport", checkNotNull(gmsCoreSupportPatch.description))
    }
}

@Suppress("unused")
val gmsCoreSupportPatch: BytecodePatch = gmsCoreSupportPatchFor(
    AppCompatibilities.ALIEXPRESS,
    gmsCoreSupportMarkerPatch,
    default = true,
)
