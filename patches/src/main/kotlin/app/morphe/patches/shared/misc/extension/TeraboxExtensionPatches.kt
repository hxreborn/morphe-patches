/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val teraboxSettingsExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch)
    extendWith("extensions/hxreborn/terabox-settings.mpe")
}

internal val teraboxShowBackupCountExtensionPatch = patch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-show-backup-count.mpe")
}

internal val teraboxUnlockPremiumPlusExtensionPatch = patch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-unlock-premium-plus.mpe")
}

internal val teraboxFixGoogleLoginExtensionPatch = patch {
    dependsOn(sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/terabox-fix-google-login.mpe")
}

internal val teraboxHideHomeShortcutsExtensionPatch = patch {
    extendWith("extensions/hxreborn/terabox-hide-home-shortcuts.mpe")
}

internal val teraboxSortByOriginalDateExtensionPatch = patch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-sort-by-original-date.mpe")
}

internal val teraboxRemoveRecentPanelExtensionPatch = patch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-remove-recent-panel.mpe")
}
