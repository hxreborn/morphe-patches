/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object CreatePostRetakeCounterSetterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("L", "I"),
    filters = listOf(fieldAccess(name = "retakeCounter_", type = "I", opcode = Opcode.IPUT)),
)

internal fun createPostRequestBuilderFingerprint(retakeCounterSetter: MethodReference) = Fingerprint(
    isStatic = true,
    returnType = retakeCounterSetter.definingClass,
    filters = listOf(methodCall(reference = retakeCounterSetter)),
)

internal fun createPostLocationReadFingerprint(locationType: String) = Fingerprint(
    filters = listOf(fieldAccess(type = locationType, opcode = Opcode.IGET_OBJECT)),
)

internal object LocationToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Location(longitude=", ", latitude="),
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
