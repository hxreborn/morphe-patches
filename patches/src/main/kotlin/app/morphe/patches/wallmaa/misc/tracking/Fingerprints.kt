/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wallmaa.misc.tracking

import app.morphe.patcher.Fingerprint

internal object LevelPlayInitFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/flutter/LevelPlayMediationPlugin;",
    name = "init",
    returnType = "V",
    parameters = listOf(
        "Lio/flutter/plugin/common/MethodCall;",
        "Lio/flutter/plugin/common/MethodChannel\$Result;",
    ),
)
