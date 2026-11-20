/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val ultimeaSettingsExtensionPatch = bytecodePatch {
    dependsOn(sharedActivityCallbacksAdapterExtensionPatch)
    extendWith("extensions/hxreborn/ultimea-settings.mpe")
}

internal val ultimeaDarkThemeExtensionPatch = bytecodePatch {
    dependsOn(ultimeaSettingsExtensionPatch)
    extendWith("extensions/hxreborn/ultimea-dark-theme.mpe")
}
