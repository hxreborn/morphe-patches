/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.imo.shared.RemoteSettingFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal const val STOCK_CLOSE_FRIEND_LIMIT = 5L

private const val CLOSE_FRIEND_PICKER_CLASS =
    "Lcom/imo/android/imoim/home/me/setting/chatbubble/ChatBubbleSelectContactsView;"

internal object CloseFriendLimitFingerprint : Fingerprint(
    definingClass = CLOSE_FRIEND_PICKER_CLASS,
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(
        literal(STOCK_CLOSE_FRIEND_LIMIT),
        methodCall(definingClass = "Lcom/imo/android/imoim/relation/api/RelationInfoApi;"),
    ),
)

internal val preselectionCountCall =
    methodCall(definingClass = "Ljava/lang/Math;", name = "min", location = MatchAfterWithin(3))

internal object CloseFriendPreselectionFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = CLOSE_FRIEND_PICKER_CLASS, returnType = "I", parameters = emptyList()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        preselectionCountCall,
    ),
)

internal object FamilyGuardContactLimitFingerprint : RemoteSettingFingerprint("familyGuardInviteMaxSelectionCount")
