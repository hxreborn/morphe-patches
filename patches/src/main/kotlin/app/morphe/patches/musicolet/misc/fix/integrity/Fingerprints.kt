/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.fix.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val STORED_VERDICT_KEY = "k_i_vflta"

internal object StoredIntegrityVerdictReadFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("k_b_hdycmi"),
    filters = listOf(
        string(STORED_VERDICT_KEY),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getInt",
            parameters = listOf("Ljava/lang/String;", "I"),
            returnType = "I",
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
