/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val utorrentSettingsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/utorrent-settings.mpe")
}

internal val utorrentSettingsMenuExtensionPatch = bytecodePatch {
    dependsOn(utorrentSettingsExtensionPatch)
    extendWith("extensions/hxreborn/utorrent-settings-menu.mpe")
}

internal val utorrentFixScreenInsetsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/utorrent-fix-screen-insets.mpe")
}
