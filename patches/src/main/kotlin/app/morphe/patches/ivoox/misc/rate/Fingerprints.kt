/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ivoox.misc.rate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ShowRateDialogPreferenceFingerprint : Fingerprint(
    definingClass = "Lcom/ivoox/app/model/AppPreferences;",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        string("show_rate_dialog"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getBoolean"),
    ),
)
