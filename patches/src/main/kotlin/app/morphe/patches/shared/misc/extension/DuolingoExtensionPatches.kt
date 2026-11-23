/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val duolingoSettingsExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedPatchesMenuExtensionPatch)
    extendWith("extensions/hxreborn/duolingo-settings.mpe")
}
