/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.integrity

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.kotlin.returnUnitEarly

internal val skipClientIntegrityAttestationPatch = bytecodePatch {
    execute {
        returnUnitEarly(ClientIntegrityAttestationFingerprint.matchSingle().method)
    }
}
