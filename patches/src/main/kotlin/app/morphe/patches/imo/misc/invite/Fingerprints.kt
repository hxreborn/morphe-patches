/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.invite

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.imo.shared.RemoteSettingFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val lastInviteTimeKey = fieldAccess(name = "LAST_INVITE_SUGGEST_TIME", opcode = Opcode.SGET_OBJECT)

internal val chatRowsDelete = methodCall(
    returnType = "I",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "[Ljava/lang/String;", "Z"),
)

internal val inviteEntryClosedKey = fieldAccess(name = "INVITE_FRIENDS_ENTRANCE_CLOSED", opcode = Opcode.SGET_OBJECT)

internal object InviteDialogCooldownFingerprint : Fingerprint(
    filters = listOf(
        lastInviteTimeKey,
        methodCall(returnType = "J", parameters = listOf("Ljava/lang/Enum;", "J")),
        opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
    ),
    strings = listOf("android.intent.action.SEND"),
)

internal object InviteChatEntryFingerprint : Fingerprint(
    filters = listOf(
        chatRowsDelete,
        inviteEntryClosedKey,
        methodCall(returnType = "Z", parameters = listOf("Ljava/lang/Enum;", "Z")),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
    strings = listOf("storeInviteFriendsIfNeed", "deleteRecommendEntrance", "buid=?"),
)

internal object CallShareGuideFingerprint : RemoteSettingFingerprint("isCallShareGuideEnable")
