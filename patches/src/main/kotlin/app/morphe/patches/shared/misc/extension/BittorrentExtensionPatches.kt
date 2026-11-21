/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val bittorrentSettingsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bittorrent-settings.mpe")
}

internal val bittorrentSettingsMenuAndAmoledThemeExtensionPatch = bytecodePatch {
    dependsOn(bittorrentSettingsExtensionPatch)
    extendWith("extensions/hxreborn/bittorrent-settings-menu-and-amoled-theme.mpe")
}

internal val bittorrentFixScreenInsetsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bittorrent-fix-screen-insets.mpe")
}
