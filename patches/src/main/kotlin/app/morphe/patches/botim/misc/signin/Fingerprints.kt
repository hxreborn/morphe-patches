/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object IntegrityWarmUpFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("isPlayServicesAvailable failed (treat as unavailable): "),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/lang/Boolean;",
            name = "valueOf",
            parameters = listOf("Z"),
        ),
    ),
)
