/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val aliexpressSettingsExtensionPatch = bytecodePatch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-settings.mpe")
}

internal val aliexpressRemoveSponsoredItemsExtensionPatch = bytecodePatch {
    dependsOn(aliexpressSettingsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-remove-sponsored-items.mpe")
}

internal val aliexpressEnableImageSavingExtensionPatch = bytecodePatch {
    dependsOn(aliexpressSettingsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-enable-image-saving.mpe")
}
