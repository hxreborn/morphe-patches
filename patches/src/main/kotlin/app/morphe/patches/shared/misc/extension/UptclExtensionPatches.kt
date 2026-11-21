/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val uptclRemoveForcedUpdateExtensionPatch = patch {
    extendWith("extensions/hxreborn/uptcl-remove-forced-update.mpe")
}

internal val uptclMatchSystemBarColorExtensionPatch = patch {
    extendWith("extensions/hxreborn/uptcl-match-system-bar-color.mpe")
}
