/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.download

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterAnywhere
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object FeedPostMenuFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L", "Z"),
    strings = listOf("Copy post id"),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "sharing_service_download"),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_INTERFACE),
            parameters = listOf("I"),
            returnType = "Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        fieldAccess(opcode = Opcode.SGET_OBJECT, location = MatchAfterWithin(8)),
        opcode(Opcode.INVOKE_DIRECT_RANGE, location = MatchAfterImmediately()),
    ),
)

internal fun feedFriendMenuItemsFingerprint(feedMenuClass: String) = Fingerprint(
    definingClass = feedMenuClass,
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "reactions_screen_menu_view_profile"),
        string("Copy post id", location = MatchAfterAnywhere()),
        opcode(Opcode.INVOKE_DIRECT_RANGE, location = MatchAfterWithin(4)),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_STATIC),
            parameters = listOf("[Ljava/lang/Object;"),
            returnType = "Ljava/util/ArrayList;",
            location = MatchAfterWithin(6),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object ReactionsPostMenuFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "Ljava/lang/Object;"),
    strings = listOf("Copy post id"),
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.STRING, "sharing_service_download", location = MatchAfterImmediately()),
    ),
)

internal object DownloadImageFingerprint : Fingerprint(
    returnType = "Landroid/graphics/Bitmap;",
    parameters = listOf("Landroid/graphics/Bitmap;"),
    filters = listOf(
        literal(540.0f),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_STATIC),
            parameters = listOf("L", "Landroid/content/Context;", "F", "F", "Ljava/lang/Integer;"),
            returnType = "V",
            location = MatchAfterAnywhere(),
        ),
    ),
)

internal object MediaStoreInsertFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("Failed to copy file to "),
    filters = listOf(
        string("/BeReal"),
        string("relative_path", location = MatchAfterWithin(8)),
        methodCall(
            opcodes = listOf(Opcode.INVOKE_VIRTUAL),
            definingClass = "Landroid/content/ContentResolver;",
            name = "insert",
            location = MatchAfterWithin(4),
        ),
    ),
)
