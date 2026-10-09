/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.gms

import app.morphe.patches.raindrop.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(AppCompatibilities.RAINDROP, spoofSignaturePatch)
