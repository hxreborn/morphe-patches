/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.habitify.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.shared.misc.gms.playServicesCredentialProviderPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(
    AppCompatibilities.HABITIFY,
    removePairipProtectionPatch,
    playServicesCredentialProviderPatch,
    default = true,
)
