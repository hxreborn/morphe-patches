/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import com.android.tools.smali.dexlib2.Opcode

private const val WIDGET_ROW_CALL_DISTANCE = 8

internal object WidgetSettingsRowFingerprint : Fingerprint(
    filters = listOf(
        resourceField(ResourceType.STRING, "settings_widget_title"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC_RANGE,
            returnType = "V",
            location = MatchAfterWithin(WIDGET_ROW_CALL_DISTANCE),
        ),
    ),
)
