/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val uptclRemoveForcedUpdateExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/uptcl-remove-forced-update.mpe")
}

internal val uptclMatchSystemBarColorExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/uptcl-match-system-bar-color.mpe")
}
