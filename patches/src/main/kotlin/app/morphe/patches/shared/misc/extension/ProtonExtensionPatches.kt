/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val protonSettingsThemeExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/proton-settings-theme.mpe")
}

internal val protonSettingsExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch, sharedPatchesMenuExtensionPatch, sharedHostedComponentsExtensionPatch)
    extendWith("extensions/hxreborn/proton-settings.mpe")
}

internal val protonMaterialSwitchViewExtensionPatch = patch {
    dependsOn(protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-material-switch-view.mpe")
}

internal val protonPatchApplicationExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedProxyHandlerExtensionPatch, sharedSignatureExtensionPatch)
    extendWith("extensions/hxreborn/proton-patch-application.mpe")
}

internal val protonUpgradePromotionsExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-upgrade-promotions.mpe")
}

internal val protonUpgradePromotionRowsExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch, protonUpgradePromotionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-upgrade-promotion-rows.mpe")
}

internal val protonAccentColorPickerExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-accent-color-picker.mpe")
}

internal val protonLoadingIndicatorExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-loading-indicator.mpe")
}

internal val protonFloatingNavigationBarExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-floating-navigation-bar.mpe")
}

internal val protonUniffiCallsExtensionPatch = patch {
    dependsOn(sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-uniffi-calls.mpe")
}

internal val protonScheduledDeletionExtensionPatch = patch {
    dependsOn(protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch, protonUniffiCallsExtensionPatch)
    extendWith("extensions/hxreborn/proton-scheduled-deletion.mpe")
}

internal val protonScheduledDeletionRowsExtensionPatch = patch {
    dependsOn(protonScheduledDeletionExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-scheduled-deletion-rows.mpe")
}

internal val protonSwipeToSwitchAccountsExtensionPatch = patch {
    dependsOn(protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch, protonUniffiCallsExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-swipe-to-switch-accounts.mpe")
}

internal val protonWebMaterialSwitchExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch)
    extendWith("extensions/hxreborn/proton-web-material-switch.mpe")
}

internal val protonAmoledDarkThemeExtensionPatch = patch {
    extendWith("extensions/hxreborn/proton-amoled-dark-theme.mpe")
}

internal val protonAuthenticatorSettingsExtensionPatch = patch {
    dependsOn(protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-authenticator-settings.mpe")
}

internal val protonM3ELoadingIndicatorExtensionPatch = patch {
    dependsOn(protonLoadingIndicatorExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-m3-e-loading-indicator.mpe")
}

internal val protonAutoDeleteBannerExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonPatchApplicationExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-auto-delete-banner.mpe")
}

internal val protonMailWebContentBackgroundExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch, sharedStreamsExtensionPatch)
    extendWith("extensions/hxreborn/proton-mail-web-content-background.mpe")
}

internal val protonWebSettingsThemeExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-web-settings-theme.mpe")
}

internal val protonPassSettingsExtensionPatch = patch {
    dependsOn(protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-pass-settings.mpe")
}

internal val protonUnlockConnectionPreferencesExtensionPatch = patch {
    extendWith("extensions/hxreborn/proton-unlock-connection-preferences.mpe")
}

internal val protonMembersExtensionPatch = patch {
    extendWith("extensions/hxreborn/proton-members.mpe")
}

internal val protonFreeServerLocationsExtensionPatch = patch {
    dependsOn(protonMembersExtensionPatch)
    extendWith("extensions/hxreborn/proton-free-server-locations.mpe")
}

internal val protonHideUpgradePromotionsExtensionPatch = patch {
    dependsOn(protonMembersExtensionPatch, protonSettingsExtensionPatch, protonUpgradePromotionsExtensionPatch)
    extendWith("extensions/hxreborn/proton-hide-upgrade-promotions.mpe")
}

internal val protonMaterial3SwitchesExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/proton-material-3-switches.mpe")
}

internal val protonVpnSettingsExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-vpn-settings.mpe")
}

internal val protonSplitTunnelingRulesExtensionPatch = patch {
    dependsOn(protonSettingsExtensionPatch, protonVpnSettingsExtensionPatch)
    extendWith("extensions/hxreborn/proton-split-tunneling-rules.mpe")
}
