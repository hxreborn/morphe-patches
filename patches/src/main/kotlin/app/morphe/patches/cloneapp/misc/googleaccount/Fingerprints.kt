/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.googleaccount

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object AccountSettingsIntentFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        string("com.google.android.gms.accountsettings.action.VIEW_SETTINGS", location = MatchAfterImmediately()),
        methodCall(
            definingClass = "Landroid/content/Intent;",
            name = "<init>",
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterImmediately(),
        ),
    ),
)
