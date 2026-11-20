/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal val interstitialStateBranch = opcode(Opcode.IF_EQ, location = MatchAfterImmediately())

internal object ShowInterstitialFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    strings = listOf("loadInterstitialAdActivityResult", "ad_box_placement"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        opcode(Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        interstitialStateBranch,
        methodCall(definingClass = "Ljava/util/Objects;", name = "toString", location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/lang/Object;", name = "<init>", location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately()),
    ),
)
