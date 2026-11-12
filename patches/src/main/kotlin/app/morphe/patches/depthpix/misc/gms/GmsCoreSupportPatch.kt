/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.gms

import app.morphe.patches.depthpix.misc.installer.playStoreInstallerHeaderPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.shared.misc.gms.playServicesCredentialProviderPatch

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(
    AppCompatibilities.DEPTHPIX,
    playServicesCredentialProviderPatch,
    playStoreInstallerHeaderPatch,
)
