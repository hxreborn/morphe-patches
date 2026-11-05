/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.ultimateguitar.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.ultimateguitar.misc.pairip.restoreVirtualizedMethodsPatch

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(
    AppCompatibilities.ULTIMATE_GUITAR,
    restoreVirtualizedMethodsPatch,
    removePairipProtectionPatch,
    spoofSignaturePatch,
    default = true,
)
