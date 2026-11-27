/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.spikestats.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object MatchSummariesBannerFingerprint : Fingerprint(
    definingClass = "Lcrocusgames/com/spikestats/activities/MatchSummariesActivity;",
    filters = listOf(
        methodCall(
            name = "loadAd",
            parameters = listOf("Lcom/google/android/gms/ads/AdRequest;"),
            returnType = "V",
        ),
    ),
)

internal object MatchDetailsBannerFingerprint : Fingerprint(
    strings = listOf("ca-app-pub-2974783845334399/3180771261"),
)

internal object MatchDetailsInlineAdFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        string("ca-app-pub-2974783845334399/7977563295"),
    ),
)
