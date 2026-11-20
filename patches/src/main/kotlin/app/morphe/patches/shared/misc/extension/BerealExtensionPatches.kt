/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val berealContactsPermissionExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bereal-contacts-permission.mpe")
}

internal val berealDataCollectionExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bereal-data-collection.mpe")
}

internal val berealSponsoredPostsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bereal-sponsored-posts.mpe")
}

internal val berealUpdatePromptsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/bereal-update-prompts.mpe")
}

internal val berealSettingsExtensionPatch = bytecodePatch {
    dependsOn(berealContactsPermissionExtensionPatch, berealDataCollectionExtensionPatch, berealSponsoredPostsExtensionPatch, berealUpdatePromptsExtensionPatch, morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/bereal-settings.mpe")
}
