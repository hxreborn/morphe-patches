/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterAnywhere
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object SettingsSectionsFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("SUBSCRIPTION"),
        string("free_membership", location = MatchAfterAnywhere()),
        string("HELP", location = MatchAfterAnywhere()),
        string("SUPPORT", location = MatchAfterAnywhere()),
        opcode(Opcode.FILLED_NEW_ARRAY_RANGE, location = MatchAfterAnywhere()),
    ),
)
