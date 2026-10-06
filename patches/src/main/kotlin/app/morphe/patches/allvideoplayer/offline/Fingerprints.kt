/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.offline

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object StartNextActivityFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    strings = listOf("SplashAct_intent_Mainact"),
)

internal object ShowOfflineDialogFingerprint : Fingerprint(
    classFingerprint = StartNextActivityFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/app/Dialog;", name = "isShowing"),
        fieldAccess(name = "isShowingAd", type = "Z", opcode = Opcode.SGET_BOOLEAN),
        methodCall(definingClass = "Landroid/app/Dialog;", name = "show"),
    ),
)
