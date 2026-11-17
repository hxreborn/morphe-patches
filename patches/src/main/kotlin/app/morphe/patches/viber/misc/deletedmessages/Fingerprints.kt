/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.deletedmessages

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val DELETED_MESSAGE_EVENT = "Lcom/viber/jni/im2/CDeletedMessageMsg;"
internal const val MESSAGE_ENTITY = "Lcom/viber/voip/feature/model/main/message/MessageEntity;"
private const val SILENT_DELETED_MESSAGE_MIME = 1007
private const val DELETED_NOTICE_MIME = 1008

internal object DeletedMessageFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Z", "J", "J"),
    filters = listOf(
        methodCall(parameters = listOf("J"), returnType = MESSAGE_ENTITY),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        string("message_deleted/"),
        methodCall(parameters = listOf(MESSAGE_ENTITY), returnType = "Z"),
        opcode(Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(definingClass = MESSAGE_ENTITY, name = "getConversationId", location = MatchAfterImmediately()),
        string("message_deleted/"),
        methodCall(parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"), returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        literal(DELETED_NOTICE_MIME),
    ),
)

internal object SilentDeletedMessageFingerprint : Fingerprint(
    classFingerprint = DeletedMessageFingerprint,
    returnType = "V",
    parameters = listOf("J", "Z"),
    filters = listOf(
        literal(SILENT_DELETED_MESSAGE_MIME),
        methodCall(definingClass = MESSAGE_ENTITY, name = "hasAnyStatus", parameters = listOf("[I"), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(definingClass = MESSAGE_ENTITY, name = "getId", returnType = "J"),
        opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
        methodCall(parameters = listOf("J"), returnType = "I", location = MatchAfterImmediately()),
    ),
)

internal object SilentMessageLookupFingerprint : Fingerprint(
    classFingerprint = DeletedMessageFingerprint,
    returnType = "V",
    parameters = listOf("J", "Z"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        methodCall(name = "get", parameters = emptyList(), returnType = "Ljava/lang/Object;", location = MatchAfterImmediately()),
        methodCall(parameters = listOf("J"), returnType = MESSAGE_ENTITY),
        literal(SILENT_DELETED_MESSAGE_MIME),
        methodCall(definingClass = MESSAGE_ENTITY, name = "hasAnyStatus", parameters = listOf("[I"), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(definingClass = MESSAGE_ENTITY, name = "getId", returnType = "J"),
        opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
        methodCall(parameters = listOf("J"), returnType = "I", location = MatchAfterImmediately()),
    ),
)

internal object DeleteForEveryoneControllerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/util/Set;", "Z"),
    filters = listOf(string("Delete without Trace"), string("Delete for everyone")),
)

internal object DeleteMessageRequestFingerprint : Fingerprint(
    classFingerprint = DeleteForEveryoneControllerFingerprint,
    returnType = "V",
    parameters = listOf("Ljava/util/Set;", "Z"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/viber/jni/im2/CDeleteMessageMsg;",
            name = "<init>",
            parameters = listOf("I", "Ljava/lang/String;", "J", "J", "I", "Z"),
            returnType = "V",
        ),
    ),
)

internal object DeletedMessageEventFingerprint : Fingerprint(
    name = "onCDeletedMessageMsg",
    returnType = "V",
    parameters = listOf(DELETED_MESSAGE_EVENT),
    filters = listOf(
        methodCall(parameters = listOf("J", "Z"), returnType = "V"),
        methodCall(parameters = listOf("Ljava/lang/String;", "Z", "J", "J"), returnType = "V"),
    ),
)
