/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tunneldoom.misc.time

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object CountdownStartFingerprint : Fingerprint(
    name = "start",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/os/SystemClock;", name = "elapsedRealtime"),
        methodCall(definingClass = "Landroid/os/Handler;", name = "sendMessage"),
    ),
)
