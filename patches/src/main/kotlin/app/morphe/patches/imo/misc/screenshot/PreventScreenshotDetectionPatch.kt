/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.screenshot

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val preventScreenshotDetectionPatch = bytecodePatch(
    name = "Prevent screenshot detection",
    description = "Stops chats and calls from reporting screenshots and screen recordings to the other person.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        listOf(
            RegisterCaptureCallbacksFingerprint,
            DispatchScreenshotFingerprint,
            RegisterCaptureObserverFingerprint,
            RegisterImageCaptureObserverFingerprint,
            RegisterCallCaptureObserverFingerprint,
        ).forEach { it.matchSingle().method.returnEarly() }
    }
}
