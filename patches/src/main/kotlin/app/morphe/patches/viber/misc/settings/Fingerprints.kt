/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object ApplicationContextFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/ManifestViberApplication;",
    name = "attachBaseContext",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

internal object PlusAdSwitchFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lcom/viber/voip/feature/viberplus/presentation/settings/ViberPlusSettingsState;",
            name = "isAdsFree",
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Landroidx/appcompat/widget/SwitchCompat;",
            name = "setChecked",
            parameters = listOf("Z"),
            location = MatchAfterImmediately(),
        ),
    ),
)
