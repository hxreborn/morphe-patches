/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.liapp

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object LiappTamperAlertFingerprint : Fingerprint(
    definingClass = "Lcom/liapp/x;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(smali = "Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;"),
        methodCall(
            smali = "Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
        ),
        methodCall(smali = "Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z"),
    ),
)

internal object LiappInitFingerprint : Fingerprint(
    definingClass = "Lcom/liapp/x;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(smali = "Landroid/os/Debug;->isDebuggerConnected()Z"),
        methodCall(smali = "Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V"),
    ),
)
