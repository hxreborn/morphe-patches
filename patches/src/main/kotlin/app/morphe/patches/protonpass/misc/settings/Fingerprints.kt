/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

internal object ApplicationSectionFingerprint : Fingerprint(
    name = "ApplicationSection",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "settings_option_view_logs"),
        methodCall(name = "SettingOption", opcode = Opcode.INVOKE_STATIC_RANGE),
        methodCall(name = "PassDivider", opcode = Opcode.INVOKE_STATIC),
    ),
)
