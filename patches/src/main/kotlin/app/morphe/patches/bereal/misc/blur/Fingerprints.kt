/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.blur

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object FeedPlacementEnumFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("FriendFeed", "FoF"),
)

internal fun postVisibilityFlowConstructorFingerprint(placementType: String) = Fingerprint(
    name = "<init>",
    returnType = "V",
    parameters = listOf("L", "L", "Ljava/lang/String;", "L", "Z", "L", "L", "L", "L", placementType, "L"),
)

internal fun postVisibilityFlowFingerprint(flowType: String) = Fingerprint(
    definingClass = flowType,
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
