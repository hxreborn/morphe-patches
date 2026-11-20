/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val vpnsuperAppliedPatchesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/vpnsuper-applied-patches.mpe")
}

internal val vpnsuperSettingsExtensionPatch = bytecodePatch {
    dependsOn(vpnsuperAppliedPatchesExtensionPatch)
    extendWith("extensions/hxreborn/vpnsuper-settings.mpe")
}
