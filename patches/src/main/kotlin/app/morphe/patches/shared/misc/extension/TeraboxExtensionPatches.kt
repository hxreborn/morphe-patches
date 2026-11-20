/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val teraboxSettingsExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch)
    extendWith("extensions/hxreborn/terabox-settings.mpe")
}

internal val teraboxShowBackupCountExtensionPatch = bytecodePatch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-show-backup-count.mpe")
}

internal val teraboxUnlockPremiumPlusExtensionPatch = bytecodePatch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-unlock-premium-plus.mpe")
}

internal val teraboxFixGoogleLoginExtensionPatch = bytecodePatch {
    dependsOn(sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/terabox-fix-google-login.mpe")
}

internal val teraboxHideHomeShortcutsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/terabox-hide-home-shortcuts.mpe")
}

internal val teraboxSortByOriginalDateExtensionPatch = bytecodePatch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-sort-by-original-date.mpe")
}

internal val teraboxRemoveRecentPanelExtensionPatch = bytecodePatch {
    dependsOn(teraboxSettingsExtensionPatch)
    extendWith("extensions/hxreborn/terabox-remove-recent-panel.mpe")
}
