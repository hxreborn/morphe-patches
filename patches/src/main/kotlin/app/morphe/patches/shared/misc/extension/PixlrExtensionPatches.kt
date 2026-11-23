/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val pixlrHideHomeSectionsExtensionPatch = patch {
    extendWith("extensions/hxreborn/pixlr-hide-home-sections.mpe")
}
