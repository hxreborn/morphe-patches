/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/aboutconfig/EnableAboutConfigPatch.kt
 */
package app.morphe.patches.firefox.misc.aboutconfig

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall

internal object AboutConfigEnabledFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/geckoview/GeckoRuntimeSettings\$Builder;",
    name = "aboutConfigEnabled",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(definingClass = "Lorg/mozilla/geckoview/GeckoRuntimeSettings;", name = "mAboutConfig"),
        methodCall(definingClass = "Lorg/mozilla/geckoview/RuntimeSettings\$Pref;", name = "set"),
    ),
)
