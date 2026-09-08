/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object IsContentUnlockedFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Lcom/ipnossoft/api/featuremanager/FeatureType;", "Ljava/lang/String;"),
    strings = listOf("*"),
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
        DATA_SOURCE_NAME_CLASS,
        PURCHASE_TYPE_CLASS,
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
