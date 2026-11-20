/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val cxDarkThemeExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/cx-dark-theme.mpe")
}

internal val cxAmoledDarkThemeExtensionPatch = bytecodePatch {
    dependsOn(cxDarkThemeExtensionPatch, sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/cx-amoled-dark-theme.mpe")
}
