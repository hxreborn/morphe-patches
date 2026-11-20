/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val bittorrentSettingsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bittorrent-settings.mpe")
}

internal val bittorrentSettings2ExtensionPatch = bytecodePatch {
    dependsOn(bittorrentSettingsExtensionPatch)
    extendWith("extensions/hxreborn/bittorrent-settings-2.mpe")
}

internal val bittorrentFixScreenInsetsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bittorrent-fix-screen-insets.mpe")
}
