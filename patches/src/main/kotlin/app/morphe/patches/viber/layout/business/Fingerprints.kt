/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.business

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object ChatSelectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("[conversations].[deleted] = 0 and "),
        string("[conversations].[grouping_key] = 'customers_inbox_'||?"),
    ),
)

internal object BusinessInboxPreferencesFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("pref_business_chat_inbox_show_intro_dialog_ftue"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun businessPreferenceReaderFingerprint(preferenceType: String) = Fingerprint(
    definingClass = preferenceType,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(type = "Ljava/lang/String;"),
        methodCall(name = "getBoolean", parameters = listOf("Ljava/lang/String;", "Z"), returnType = "Z"),
    ),
)
