/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val liftoffSettingsExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/liftoff-settings.mpe")
}

internal val liftoffUnlockCustomizationsExtensionPatch = patch {
    dependsOn(liftoffSettingsExtensionPatch)
    extendWith("extensions/hxreborn/liftoff-unlock-customizations.mpe")
}
