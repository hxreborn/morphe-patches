/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.gms

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.shared.misc.gms.playServicesCredentialProviderPatch

@Suppress("unused")
val gmsCoreSupportPatch: BytecodePatch = gmsCoreSupportPatchFor(
    AppCompatibilities.AUDIOMACK,
    playServicesCredentialProviderPatch,
    default = true,
)
