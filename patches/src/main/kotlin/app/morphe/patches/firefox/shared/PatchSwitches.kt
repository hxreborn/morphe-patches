/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS

internal fun BytecodePatchContext.listInSheet(feature: String) {
    mutableClassDefBy(PATCH_SETTINGS).methods.single { it.name == "${feature}Applied" }.returnEarly(true)
}
