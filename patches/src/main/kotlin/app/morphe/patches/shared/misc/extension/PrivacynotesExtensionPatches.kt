/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val privacynotesUnlockPremiumExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch)
    extendWith("extensions/hxreborn/privacynotes-unlock-premium.mpe")
}
