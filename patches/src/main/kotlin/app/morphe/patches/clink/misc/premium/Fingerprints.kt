/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clink.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object PublishEntitlementFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        fieldAccess(definingClass = "this", type = "Z", opcode = Opcode.IGET_BOOLEAN),
        string("purchased", location = MatchAfterWithin(2)),
        string("shared-subscription"),
    ),
)

internal object LoadStoredEntitlementFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("debugPro"),
        string("storedVerdict"),
        string("shared-subscription"),
    ),
)

internal object ApplyPurchasesFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/ArrayList;", "Ljava/util/Set;", "J", "Z"),
    filters = listOf(
        string("ltd.anti.clink.lifetime"),
        methodCall(definingClass = "Ljava/util/Set;", name = "contains", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
