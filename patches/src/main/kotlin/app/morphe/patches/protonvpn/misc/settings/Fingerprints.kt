/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val WIDGET_ROW_CALL_DISTANCE = 8

internal object WidgetSettingsRowFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/protonvpn/android/R\$string;",
            name = "settings_widget_title",
            opcode = Opcode.SGET,
        ),
        methodCall(
            name = "SettingRowWithIcon",
            opcode = Opcode.INVOKE_STATIC_RANGE,
            location = MatchAfterWithin(WIDGET_ROW_CALL_DISTANCE),
        ),
    ),
)
