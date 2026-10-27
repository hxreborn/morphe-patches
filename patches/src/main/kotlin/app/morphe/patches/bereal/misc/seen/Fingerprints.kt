/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.seen

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MarkPostsAsSeenRpcFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "L", "L"),
    strings = listOf("MarkPostsAsSeen"),
)

internal object EnterChatOperationFingerprint : Fingerprint(
    name = "dynamicMethod",
    strings = listOf("enterChat_"),
)

private fun successReturnFilters() = listOf(
    newInstance("L"),
    fieldAccess(opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
    methodCall(name = "<init>", parameters = listOf("Ljava/lang/Object;"), location = MatchAfterImmediately()),
    opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
)

internal fun markPostsAsSeenRepositoryFingerprint(requestType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/util/ArrayList;", "L"),
    filters = listOf(
        methodCall(definingClass = requestType, parameters = listOf(requestType, "Ljava/lang/Iterable;")),
    ) + successReturnFilters(),
)

internal fun enterChatRepositoryFingerprint(operationType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "Ljava/lang/String;", "Z"),
    filters = listOf(
        methodCall(definingClass = operationType, parameters = listOf(operationType, "Z")),
    ) + successReturnFilters(),
)
