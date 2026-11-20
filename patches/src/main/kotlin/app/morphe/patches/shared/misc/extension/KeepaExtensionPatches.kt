/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val keepaMultipleAccountsExtensionPatch = bytecodePatch {
    dependsOn(sharedNightModeExtensionPatch)
    extendWith("extensions/hxreborn/keepa-multiple-accounts.mpe")
}
