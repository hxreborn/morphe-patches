/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.playwire

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly

internal val stopPlaywireSdkPatch = patch {
    execute {
        StartPlaywireSdkFingerprint.method.returnEarly()
    }
}
