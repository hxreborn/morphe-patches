/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE

internal object ColorSchemeGetterFingerprint : Fingerprint(
    name = "getColorScheme",
    parameters = listOf(COMPOSER_TYPE, "I"),
    filters = listOf(methodCall(name = "getLocalThemeColorScheme")),
)
