/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val botimRemoveHomeSectionsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/botim-remove-home-sections.mpe")
}

internal val botimMainTabsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/botim-main-tabs.mpe")
}

internal val botimUnlockVipExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/botim-unlock-vip.mpe")
}
