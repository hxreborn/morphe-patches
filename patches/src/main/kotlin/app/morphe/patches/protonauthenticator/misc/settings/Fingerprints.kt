/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object ViewLogsRowFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(name = "settings_application_title_view_logs", type = "I", opcode = Opcode.SGET),
        methodCall(
            name = "NavigationRow",
            returnType = "V",
            opcode = Opcode.INVOKE_STATIC_RANGE,
        ),
    ),
)

internal object HorizontalDividerFingerprint : Fingerprint(
    definingClass = "Landroidx/compose/material3/DividerKt;",
    name = "HorizontalDivider-9IZ8Weo",
)
