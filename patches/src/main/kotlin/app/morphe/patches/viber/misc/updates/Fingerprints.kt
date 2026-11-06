/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object UpdatePromptConfigFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("versionDays:")),
)

internal fun updatePromptCheckFingerprint(configType: String) = Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = configType, type = "Z"),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = configType, type = "Z"),
        string("In App Update Dialog Displayed"),
    ),
)
