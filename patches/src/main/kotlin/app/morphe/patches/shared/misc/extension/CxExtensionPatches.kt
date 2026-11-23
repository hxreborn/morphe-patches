/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

internal val cxDarkThemeExtensionPatch = extensionPayload("cx-dark-theme")

internal val cxAmoledDarkThemeExtensionPatch =
    extensionPayload("cx-amoled-dark-theme", cxDarkThemeExtensionPatch, sharedNightModeExtensionPatch)
