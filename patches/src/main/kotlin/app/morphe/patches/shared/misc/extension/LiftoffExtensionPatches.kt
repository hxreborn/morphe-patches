/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

internal val liftoffSettingsExtensionPatch = extensionPayload(
    "liftoff-settings",
    morpheLibraryCoreExtensionPatch,
    sharedActivityCallbacksAdapterExtensionPatch,
    sharedDimensionsExtensionPatch,
    sharedPatchesMenuExtensionPatch,
)

internal val liftoffUnlockCustomizationsExtensionPatch =
    extensionPayload("liftoff-unlock-customizations", liftoffSettingsExtensionPatch)
