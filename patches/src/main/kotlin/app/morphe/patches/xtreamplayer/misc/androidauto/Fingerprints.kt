/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.androidauto

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object CheckUpdatesFingerprint : Fingerprint(
    parameters = listOf("Z"),
    returnType = "V",
    strings = listOf("DELETE_ON_STARTUP", "https://api.github.com/repos/malebuffy/XtreamPlayer/releases/latest"),
)

internal object MainActivityResumeFingerprint : Fingerprint(
    classFingerprint = CheckUpdatesFingerprint,
    name = "onResume",
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(methodCall(opcode = Opcode.INVOKE_SUPER, name = "onResume")),
)
