/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blockit.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val EDITOR_CLASS = "Landroid/content/SharedPreferences\$Editor;"

internal object OwnedPurchasesFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/util/List;"),
    strings = listOf("owned INAPP purchases: "),
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.IPUT_BOOLEAN),
        string("is_ad_remove", location = MatchAfterWithin(3)),
        methodCall(definingClass = EDITOR_CLASS, name = "putBoolean"),
        string("sounds_owned"),
        methodCall(definingClass = EDITOR_CLASS, name = "putBoolean", location = MatchAfterImmediately()),
    ),
)
