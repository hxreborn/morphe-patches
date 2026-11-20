/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val idlefishAllowSharingWithoutLoginExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/idlefish-allow-sharing-without-login.mpe")
}
