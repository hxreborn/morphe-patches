/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object CreatePostRetakeCounterSetterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("L", "I"),
    filters = listOf(fieldAccess(name = "retakeCounter_", type = "I", opcode = Opcode.IPUT)),
)

internal fun createPostTakenAtSetterFingerprint(requestType: String) = Fingerprint(
    definingClass = requestType,
    isStatic = true,
    returnType = "V",
    parameters = listOf(requestType, "L"),
    filters = listOf(fieldAccess(definingClass = requestType, name = "takenAt_", opcode = Opcode.IPUT_OBJECT)),
)

internal fun createPostRequestBuilderFingerprint(retakeCounterSetter: MethodReference) = Fingerprint(
    isStatic = true,
    returnType = retakeCounterSetter.definingClass,
    filters = listOf(methodCall(reference = retakeCounterSetter)),
)

internal fun createPostLocationReadFingerprint(locationType: String) = Fingerprint(
    filters = listOf(fieldAccess(type = locationType, opcode = Opcode.IGET_OBJECT)),
)

internal fun createPostTakenAtConversionFingerprint(
    corePostType: String,
    instantType: String,
    timestampType: String,
    takenAtSetter: MethodReference,
) = Fingerprint(
    filters = listOf(
        fieldAccess(definingClass = corePostType, type = instantType, opcode = Opcode.IGET_OBJECT),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(instantType),
            returnType = timestampType,
            location = MatchAfterWithin(4),
        ),
        methodCall(reference = takenAtSetter, location = MatchAfterWithin(4)),
    ),
)

internal object CorePostToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("CorePost(id="),
    filters = listOf(
        string(", isMain="),
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN, location = MatchAfterWithin(6)),
    ),
)

internal object FusedLocationClientGetterFingerprint : Fingerprint(
    returnType = "Lcom/google/android/gms/location/FusedLocationProviderClient;",
    parameters = emptyList(),
    filters = listOf(checkCast("Lcom/google/android/gms/location/FusedLocationProviderClient;")),
)

internal object LocationToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Location(longitude=", ", latitude="),
)

internal object MomentToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Moment(id=", ", creationDate=", ", expirationDate="),
)

internal fun lateCheckFingerprint(momentType: String) = Fingerprint(
    returnType = "Ljava/io/Serializable;",
    filters = listOf(checkCast(momentType)),
)

internal object SendPostDelegateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "L", "L", "L", "I", "L", "L", "L", "Ljava/lang/String;", "Ljava/util/Set;",
        "L", "Z", "Z", "L", "L", "L", "L", "L",
    ),
    strings = listOf("SendBeRealDelegate.sendBeReal failed"),
    filters = listOf(opcode(Opcode.INSTANCE_OF)),
)

internal fun sendPostContinuationConstructionFingerprint(continuationType: String) = Fingerprint(
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_DIRECT, definingClass = continuationType, name = "<init>"),
    ),
)
