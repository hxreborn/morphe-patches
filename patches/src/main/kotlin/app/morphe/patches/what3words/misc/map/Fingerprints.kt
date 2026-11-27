/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.what3words.misc.map

import app.morphe.patcher.Fingerprint

internal object GetMapTypeFingerprint : Fingerprint(
    definingClass = "Lcom/what3words/preferences/AppPrefsManager;",
    name = "getMapType",
    returnType = "I",
    parameters = emptyList(),
)
