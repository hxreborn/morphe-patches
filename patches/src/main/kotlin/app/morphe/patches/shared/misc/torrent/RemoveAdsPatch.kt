/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.torrent

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

internal val removeAdsPatch = patch {
    compatibleWith(AppCompatibilities.BITTORRENT, AppCompatibilities.UTORRENT)
    dependsOn(removeProtectionPatch)

    execute {
        InitializeAdsFingerprint.matchSingle().method.returnEarly()
    }
}
