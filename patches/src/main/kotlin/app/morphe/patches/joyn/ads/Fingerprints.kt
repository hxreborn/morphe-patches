/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.joyn.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object AdsDisabledLogMessageFingerprint : Fingerprint(
    filters = listOf(string("Ads are disabled; skipping Ad SDK setup on the player")),
)

internal fun enableAdsCheckFingerprint(playerFactoryType: String) = Fingerprint(
    definingClass = playerFactoryType,
    filters = listOf(
        methodCall(definingClass = "Lkotlin/jvm/functions/Function0;", name = "invoke"),
        methodCall(
            definingClass = "Ljava/lang/Boolean;",
            name = "booleanValue",
            location = MatchAfterWithin(3),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
