/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.feed

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val KOTLIN_INTRINSICS_CLASS = "Lkotlin/jvm/internal/Intrinsics;"
private const val ENUM_CLASS = "Ljava/lang/Enum;"
private const val ENUM_CONSTRUCTOR_WINDOW = 2
private const val APPENDED_PROPERTY_WINDOW = 3

private fun parameterNullCheck() = methodCall(
    definingClass = KOTLIN_INTRINSICS_CLASS,
    name = "checkNotNullParameter",
    location = MatchAfterImmediately(),
)

internal object FeedStateConstructorFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        string("topicChips"),
        parameterNullCheck(),
        string("cardList"),
        parameterNullCheck(),
    ),
    strings = listOf("deviceId", "headerMap"),
)

internal object FeedStatePropertyOrderFingerprint : Fingerprint(
    classFingerprint = FeedStateConstructorFingerprint,
    name = "toString",
    filters = listOf(
        string(", topicChips="),
        string(", selectedTopicInternalId=", location = MatchAfterWithin(APPENDED_PROPERTY_WINDOW)),
    ),
)

internal object FeedCardModelFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(string("cardType"), parameterNullCheck()),
    strings = listOf("shareImageUrl", "byLineLink", "cardImageUrl"),
)

internal fun feedCardTypeConstantFingerprint(cardTypeClass: String, constantName: String) = Fingerprint(
    definingClass = cardTypeClass,
    name = "<clinit>",
    filters = listOf(
        string(constantName),
        methodCall(
            definingClass = ENUM_CLASS,
            name = "<init>",
            location = MatchAfterWithin(ENUM_CONSTRUCTOR_WINDOW),
        ),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun toImmutableListFingerprint(listType: String) = Fingerprint(
    isStatic = true,
    returnType = listType,
    parameters = listOf("Ljava/lang/Iterable;"),
    filters = listOf(checkCast(listType)),
    strings = listOf("<this>"),
)
