/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val utorrentSettingsExtensionPatch = patch {
    dependsOn(sharedPatchesMenuExtensionPatch)
    extendWith("extensions/hxreborn/utorrent-settings.mpe")
}

internal val utorrentSettingsMenuExtensionPatch = patch {
    dependsOn(utorrentSettingsExtensionPatch)
    extendWith("extensions/hxreborn/utorrent-settings-menu.mpe")
}

internal val utorrentFixScreenInsetsExtensionPatch = patch {
    extendWith("extensions/hxreborn/utorrent-fix-screen-insets.mpe")
}
