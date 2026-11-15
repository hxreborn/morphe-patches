/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shttps.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val STRING = "Ljava/lang/String;"
private const val SHARED_PREFERENCES = "Landroid/content/SharedPreferences;"

internal object StartupLicenseCheckFingerprint : Fingerprint(
    filters = listOf(
        string("key_user_subscription"),
        methodCall(parameters = listOf(STRING, STRING), returnType = "Z", opcode = Opcode.INVOKE_STATIC),
        string("key_user_subscription_confirmed"),
        methodCall(definingClass = SHARED_PREFERENCES, name = "getBoolean", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
