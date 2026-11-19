/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.shared

import app.morphe.patcher.Fingerprint

private const val NATIVE_APPLICATION = "Lcom/king/core/gameactivity/NativeApplication;"

internal object NativeApplicationHandleSetterFingerprint : Fingerprint(
    definingClass = NATIVE_APPLICATION,
    name = "setNativeHandle",
    returnType = "V",
    parameters = listOf("J"),
)

internal object NativeApplicationGameThreadFingerprint : Fingerprint(
    definingClass = NATIVE_APPLICATION,
    name = "runOnGameThread",
    returnType = "V",
    parameters = listOf("Ljava/lang/Runnable;"),
)
