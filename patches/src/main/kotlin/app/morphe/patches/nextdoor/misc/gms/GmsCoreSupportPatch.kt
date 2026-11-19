/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.misc.gms

import app.morphe.patches.nextdoor.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.shared.misc.gms.playServicesCredentialProviderPatch

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(
    AppCompatibilities.NEXTDOOR,
    playServicesCredentialProviderPatch,
    spoofSignaturePatch,
    default = true,
)
