/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.tracking

import app.morphe.patcher.Fingerprint

internal object DefaultSchedulerScheduleFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/datatransport/runtime/scheduling/DefaultScheduler;",
    name = "schedule",
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/datatransport/runtime/TransportContext;",
        "Lcom/google/android/datatransport/runtime/EventInternal;",
        "Lcom/google/android/datatransport/TransportScheduleCallback;",
    ),
)
