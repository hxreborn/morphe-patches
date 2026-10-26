/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iototoy.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object AppBrainInitFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Z"),
    filters = listOf(
        string("multi-call to AppBrainPrefs.init()?"),
    ),
)

internal object ScheduleTransportEventFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("' is not registered"),
        string("Error scheduling event "),
    ),
)
