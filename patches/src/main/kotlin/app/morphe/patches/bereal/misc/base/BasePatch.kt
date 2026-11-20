/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.base

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.bereal.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

internal val beRealBasePatch = bytecodePatch {
    dependsOn(removePairipProtectionPatch, spoofSignaturePatch)
}
