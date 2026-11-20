/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val biwengerFetchRewriteExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/biwenger-fetch-rewrite.mpe")
}

internal val biwengerRemoveAdsExtensionPatch = bytecodePatch {
    dependsOn(biwengerFetchRewriteExtensionPatch)
    extendWith("extensions/hxreborn/biwenger-remove-ads.mpe")
}

internal val biwengerUnlockPremiumExtensionPatch = bytecodePatch {
    dependsOn(biwengerFetchRewriteExtensionPatch)
    extendWith("extensions/hxreborn/biwenger-unlock-premium.mpe")
}
