/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.gms

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.steezy.misc.fix.signature.spoofSignaturePatch

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(AppCompatibilities.STEEZY, spoofSignaturePatch)
