/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val aliexpressSettingsExtensionPatch = patch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-settings.mpe")
}

internal val aliexpressRemoveSponsoredItemsExtensionPatch = patch {
    dependsOn(aliexpressSettingsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-remove-sponsored-items.mpe")
}

internal val aliexpressEnableImageSavingExtensionPatch = patch {
    dependsOn(aliexpressSettingsExtensionPatch)
    extendWith("extensions/hxreborn/aliexpress-enable-image-saving.mpe")
}
