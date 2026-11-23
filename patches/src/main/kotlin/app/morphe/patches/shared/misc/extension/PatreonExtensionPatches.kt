/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val patreonSettingsExtensionPatch = patch {
    dependsOn(sharedProxyHandlerExtensionPatch, sharedPatchesMenuExtensionPatch, sharedHostedComponentsExtensionPatch)
    extendWith("extensions/hxreborn/patreon-settings.mpe")
}

internal val patreonFeatureFlagsExtensionPatch = patch {
    dependsOn(patreonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/patreon-feature-flags.mpe")
}
