/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.pairip

import app.morphe.patcher.Fingerprint

internal object GuardedAsyncTaskDoInBackgroundFingerprint : Fingerprint(
    definingClass = GUARDED_ASYNC_TASK,
    name = DO_IN_BACKGROUND,
    returnType = "Ljava/lang/Void;",
)
