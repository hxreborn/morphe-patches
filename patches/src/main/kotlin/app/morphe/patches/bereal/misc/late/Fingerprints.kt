/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.late

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private fun lateFlagReadFilters() = listOf(
    string(", isLate="),
    fieldAccess(definingClass = "this", opcode = Opcode.IGET_BOOLEAN),
    methodCall(location = MatchAfterImmediately()),
)

internal object CorePostToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("CorePost(id=", ", lateBy="),
    filters = lateFlagReadFilters(),
)

internal object MomentPostToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("MomentPost(conversationId="),
    filters = lateFlagReadFilters(),
)

internal object ChatPostToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Post(ownerAvatarInfo="),
    filters = lateFlagReadFilters(),
)

internal object PostPlaceholderToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("PostPlaceHolder(userName=", ", lateInSeconds="),
)

internal fun placeholderLateSecondsGetterFingerprint(placeholderType: String) = Fingerprint(
    definingClass = placeholderType,
    returnType = "Ljava/lang/Integer;",
    parameters = emptyList(),
    filters = listOf(fieldAccess(name = "lateInSeconds", opcode = Opcode.IGET_OBJECT)),
)

internal object LateDurationFormatterFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf("J"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "timelineCell_late_of")),
)

internal object LateDurationLiteralFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "timelineCell_late_of")),
)

internal object LateTagLiteralFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "general_late")),
)

internal object PostedLateLiteralFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "group_notification_posted_late")),
)

internal fun lateReadFingerprint(definingClass: String, name: String, opcode: Opcode) = Fingerprint(
    filters = listOf(fieldAccess(definingClass = definingClass, name = name, opcode = opcode)),
)
