/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val vpnsuperAppliedPatchesExtensionPatch = patch {
    extendWith("extensions/hxreborn/vpnsuper-applied-patches.mpe")
}

internal val vpnsuperSettingsExtensionPatch = patch {
    dependsOn(vpnsuperAppliedPatchesExtensionPatch, sharedPatchesMenuExtensionPatch, sharedHostedComponentsExtensionPatch)
    extendWith("extensions/hxreborn/vpnsuper-settings.mpe")
}
