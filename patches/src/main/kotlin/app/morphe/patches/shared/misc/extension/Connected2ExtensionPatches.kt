/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val connected2KeepDeletedMessagesExtensionPatch = patch {
    extendWith("extensions/hxreborn/connected2-keep-deleted-messages.mpe")
}
