/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val raindropAccentColorExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/raindrop-accent-color.mpe")
}

internal val raindropUnlockProExtensionPatch = bytecodePatch {
    dependsOn(sharedStreamsExtensionPatch)
    extendWith("extensions/hxreborn/raindrop-unlock-pro.mpe")
}
