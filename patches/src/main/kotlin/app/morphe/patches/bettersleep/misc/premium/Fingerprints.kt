/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object IsContentUnlockedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "Ljava/lang/String;"),
    filters = listOf(
        string("*"),
        methodCall(definingClass = "Ljava/util/List;", name = "contains", location = MatchAfterImmediately()),
    ),
)

internal object OnAskForUserPremiumFingerprint : Fingerprint(
    definingClass = "Lipnossoft/rma/free/rapidApps/onboarding/RapidOnboardingEventListener;",
    name = "onAskForUserPremium",
    returnType = "Z",
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
)

internal object PurchaseConstructorFingerprint : Fingerprint(
    definingClass = PURCHASE_CLASS,
    name = "<init>",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "J",
        "J",
        "Ljava/lang/String;",
        "L",
        "L",
        "Z",
        "Z",
        "Ljava/lang/String;",
        "Ljava/lang/Long;",
        "Z",
        "Z",
        "Ljava/lang/Boolean;",
        "Ljava/lang/Long;",
        "Ljava/lang/String;",
    ),
)
