/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object GmsAccountPickerIntentFingerprint : Fingerprint(
    returnType = "Landroid/content/Intent;",
    parameters = listOf(),
    filters = listOf(
        string("allowableAccountTypes"),
        string("setGmsCoreAccount"),
    ),
)
