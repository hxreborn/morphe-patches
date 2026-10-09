/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medicneet.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val STRING = "Ljava/lang/String;"

private const val OBJECT = "Ljava/lang/Object;"

private const val LIST = "Ljava/util/List;"

private const val MAP = "Ljava/util/Map;"

private const val ARRAY_LIST = "Ljava/util/ArrayList;"

private const val BOOLEAN = "Ljava/lang/Boolean;"

private const val TASKS = "Lcom/google/android/gms/tasks/Tasks;"

private const val FIRESTORE = "Lcom/google/firebase/firestore/FirebaseFirestore;"

internal object FirestoreQueryGetRunFingerprint : Fingerprint(
    name = "run",
    returnType = "V",
    strings = listOf("invalid_query"),
    filters = listOf(
        fieldAccess(definingClass = "this", type = STRING, opcode = Opcode.IGET_OBJECT),
        fieldAccess(
            definingClass = "this",
            type = BOOLEAN,
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        fieldAccess(
            definingClass = "this",
            type = "L",
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_EXCEPTION),
        methodCall(definingClass = TASKS, name = "await"),
    ),
)

internal object FirestoreQueryParserFingerprint : Fingerprint(
    returnType = "L",
    strings = listOf("FLTFirestoreMsgCodec"),
    filters = listOf(
        fieldAccess(definingClass = "L", type = LIST, opcode = Opcode.IGET_OBJECT),
        methodCall(
            definingClass = "Ljava/util/Objects;",
            name = "requireNonNull",
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = LIST, name = "get", parameters = listOf("I")),
        checkCast("L", location = MatchAfterWithin(2)),
    ),
)

internal fun fieldPathFactoryFingerprint(fieldPathType: String) = Fingerprint(
    definingClass = fieldPathType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = fieldPathType,
    parameters = listOf(STRING),
)

internal object FirestoreDocumentGetRunFingerprint : Fingerprint(
    name = "run",
    returnType = "V",
    filters = listOf(
        fieldAccess(definingClass = "this", type = OBJECT, opcode = Opcode.IGET_OBJECT),
        checkCast("L", location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = "this",
            type = OBJECT,
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        checkCast("L", location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = "this",
            type = OBJECT,
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        checkCast("L", location = MatchAfterImmediately()),
        fieldAccess(definingClass = "L", type = STRING, opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(8)),
        methodCall(
            definingClass = FIRESTORE,
            parameters = listOf(STRING),
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = TASKS, name = "await", location = MatchAfterWithin(4)),
        checkCast("L", location = MatchAfterWithin(2)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I"), location = MatchAfterWithin(3)),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L", "L"),
            returnType = "L",
            location = MatchAfterWithin(3),
        ),
        fieldAccess(type = ARRAY_LIST, opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(4)),
        methodCall(
            definingClass = ARRAY_LIST,
            name = "add",
            parameters = listOf("I", OBJECT),
            location = MatchAfterWithin(2),
        ),
        fieldAccess(type = "L", opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(OBJECT),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_EXCEPTION, location = MatchAfterWithin(3)),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L", "Ljava/lang/Exception;"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object FirestoreSnapshotMessageFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "L"),
        fieldAccess(type = STRING, opcode = Opcode.IPUT_OBJECT),
        fieldAccess(type = MAP, opcode = Opcode.IPUT_OBJECT),
        fieldAccess(type = "L", opcode = Opcode.IPUT_OBJECT),
    ),
)

internal object FirestoreSnapshotMetadataFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        fieldAccess(type = BOOLEAN, opcode = Opcode.IPUT_OBJECT),
        fieldAccess(type = BOOLEAN, opcode = Opcode.IPUT_OBJECT),
    ),
)
