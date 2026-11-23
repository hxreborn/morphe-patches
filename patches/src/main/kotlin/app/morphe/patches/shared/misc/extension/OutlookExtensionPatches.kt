/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch

internal val outlookAppliedPatchesExtensionPatch = patch {
    extendWith("extensions/hxreborn/outlook-applied-patches.mpe")
}

internal val outlookSettingsExtensionPatch = patch {
    dependsOn(
        outlookAppliedPatchesExtensionPatch,
        sharedPatchesMenuExtensionPatch,
        sharedProxyHandlerExtensionPatch,
        sharedActivityCallbacksAdapterExtensionPatch,
    )
    extendWith("extensions/hxreborn/outlook-settings.mpe")
}
