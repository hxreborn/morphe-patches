/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly

internal val adRemovalEntitlementPatch = bytecodePatch {
    execute {
        HasAdRemovalFingerprint.matchSingle().method.returnEarly(true)
    }
}
