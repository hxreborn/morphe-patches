/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val patreonSettingsExtensionPatch = bytecodePatch {
    dependsOn(sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/patreon-settings.mpe")
}

internal val patreonFeatureFlagsExtensionPatch = bytecodePatch {
    dependsOn(patreonSettingsExtensionPatch)
    extendWith("extensions/hxreborn/patreon-feature-flags.mpe")
}
