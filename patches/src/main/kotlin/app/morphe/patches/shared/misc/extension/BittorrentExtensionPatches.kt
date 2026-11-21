/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val bittorrentSettingsExtensionPatch = patch {
    extendWith("extensions/hxreborn/bittorrent-settings.mpe")
}

internal val bittorrentSettingsMenuAndAmoledThemeExtensionPatch = patch {
    dependsOn(bittorrentSettingsExtensionPatch)
    extendWith("extensions/hxreborn/bittorrent-settings-menu-and-amoled-theme.mpe")
}

internal val bittorrentFixScreenInsetsExtensionPatch = patch {
    extendWith("extensions/hxreborn/bittorrent-fix-screen-insets.mpe")
}
