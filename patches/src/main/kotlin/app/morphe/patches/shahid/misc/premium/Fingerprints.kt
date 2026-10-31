/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patches.shahid.shared.PlayoutMediaToStringFingerprint
import app.morphe.patches.shahid.shared.fieldGetterFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val PlayoutMediaUrlFingerprint = fieldGetterFingerprint(PlayoutMediaToStringFingerprint, "url")

internal object ResolutionPropertyToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("Property(available=", ", eligible="),
)

internal object ResolutionEligibleFingerprint : Fingerprint(
    classFingerprint = ResolutionPropertyToStringFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(fieldAccess(opcode = Opcode.IGET_BOOLEAN, name = "eligible")),
)

internal object SubscriptionFeatureEnabledFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/AbstractMap;", name = "get"),
        methodCall(definingClass = "Ljava/lang/Iterable;", name = "iterator"),
        methodCall(definingClass = "Ljava/util/AbstractMap;", name = "get"),
        fieldAccess(definingClass = "Ljava/lang/Boolean;", name = "TRUE"),
    ),
)
