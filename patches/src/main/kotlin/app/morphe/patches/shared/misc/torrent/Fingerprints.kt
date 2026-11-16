/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.torrent

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object CertificateComparisonFingerprint : Fingerprint(
    strings = listOf("appSignatureFailure"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/String;", name = "equalsIgnoreCase"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)

internal object ProStatusFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("PRO_PAID"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal object InitializeAdsFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("initAppLovin(): initializing"),
    filters = listOf(methodCall(definingClass = "Lcom/applovin/sdk/AppLovinSdk;", name = "initialize")),
)
