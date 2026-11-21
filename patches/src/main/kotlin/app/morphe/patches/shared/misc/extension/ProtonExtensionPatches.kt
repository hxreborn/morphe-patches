/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val protonSettingsThemeExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/proton-settings-theme.mpe")
}

internal val protonSettingsExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-settings.mpe")
}

internal val protonMaterialSwitchViewExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-material-switch-view.mpe")
}

internal val protonPatchApplicationExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedProxyHandlerExtensionPatch, sharedSignatureExtensionPatch)
    extendWith("extensions/hxreborn/proton-patch-application.mpe")
}

internal val protonUpgradePromotionsExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-upgrade-promotions.mpe")
}

internal val protonUpgradePromotionRowsExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch, protonUpgradePromotionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-upgrade-promotion-rows.mpe")
}

internal val protonAccentColorPickerExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-accent-color-picker.mpe")
}

internal val protonLoadingIndicatorExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-loading-indicator.mpe")
}

internal val protonFloatingNavigationBarExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-floating-navigation-bar.mpe")
}

internal val protonUniffiCallsExtensionPatch = bytecodePatch {
    dependsOn(sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-uniffi-calls.mpe")
}

internal val protonScheduledDeletionExtensionPatch = bytecodePatch {
    dependsOn(protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch, protonUniffiCallsExtensionPatch)
    extendWith("extensions/hxreborn/proton-scheduled-deletion.mpe")
}

internal val protonScheduledDeletionRowsExtensionPatch = bytecodePatch {
    dependsOn(protonScheduledDeletionExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-scheduled-deletion-rows.mpe")
}

internal val protonSwipeToSwitchAccountsExtensionPatch = bytecodePatch {
    dependsOn(protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch, protonUniffiCallsExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-swipe-to-switch-accounts.mpe")
}

internal val protonWebMaterialSwitchExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch)
    extendWith("extensions/hxreborn/proton-web-material-switch.mpe")
}

internal val protonAmoledDarkThemeExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/proton-amoled-dark-theme.mpe")
}

internal val protonAuthenticatorSettingsExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-authenticator-settings.mpe")
}

internal val protonM3ELoadingIndicatorExtensionPatch = bytecodePatch {
    dependsOn(protonLoadingIndicatorExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-m3-e-loading-indicator.mpe")
}

internal val protonAutoDeleteBannerExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-auto-delete-banner.mpe")
}

internal val protonMailWebContentBackgroundExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch, sharedStreamsExtensionPatch)
    extendWith("extensions/hxreborn/proton-mail-web-content-background.mpe")
}

internal val protonWebSettingsThemeExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-web-settings-theme.mpe")
}

internal val protonPassSettingsExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-pass-settings.mpe")
}

internal val protonUnlockConnectionPreferencesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/proton-unlock-connection-preferences.mpe")
}

internal val protonMembersExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/proton-members.mpe")
}

internal val protonFreeServerLocationsExtensionPatch = bytecodePatch {
    dependsOn(protonMembersExtensionPatch)
    extendWith("extensions/hxreborn/proton-free-server-locations.mpe")
}

internal val protonHideUpgradePromotionsExtensionPatch = bytecodePatch {
    dependsOn(protonMembersExtensionPatch, protonSettingsExtensionPatch, protonUpgradePromotionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-hide-upgrade-promotions.mpe")
}

internal val protonMaterial3SwitchesExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-material-3-switches.mpe")
}

internal val protonVpnSettingsExtensionPatch = bytecodePatch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-vpn-settings.mpe")
}
