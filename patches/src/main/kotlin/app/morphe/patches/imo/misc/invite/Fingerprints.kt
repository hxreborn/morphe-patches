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
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.string
import app.morphe.patches.imo.shared.RemoteSettingFingerprint
import app.morphe.patches.imo.shared.enumConstantField
import com.android.tools.smali.dexlib2.Opcode

private val chatRowsDelete = methodCall(
    returnType = "I",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "[Ljava/lang/String;", "Z"),
)

context(_: BytecodePatchContext)
internal fun inviteDialogCooldownFingerprint() = Fingerprint(
    filters = listOf(
        fieldAccess(enumConstantField("LAST_INVITE_SUGGEST_TIME"), Opcode.SGET_OBJECT),
        methodCall(returnType = "J", parameters = listOf("Ljava/lang/Enum;", "J")),
        opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
        string("android.intent.action.SEND"),
    ),
)

context(_: BytecodePatchContext)
internal fun inviteChatEntryFingerprint() = Fingerprint(
    filters = listOf(
        chatRowsDelete,
        fieldAccess(enumConstantField("INVITE_FRIENDS_ENTRANCE_CLOSED"), Opcode.SGET_OBJECT),
        methodCall(returnType = "Z", parameters = listOf("Ljava/lang/Enum;", "Z")),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object CallShareGuideFingerprint : RemoteSettingFingerprint("isCallShareGuideEnable")

internal object DownloadFreeImoDialogFingerprint : RemoteSettingFingerprint("downloadFreeImoDialog")

internal object DownloadFreeImoTipsFingerprint : RemoteSettingFingerprint("downloadFreeImoTips")

internal object DownloadFreeImoTips2Fingerprint : RemoteSettingFingerprint("downloadFreeImoTips2")

internal object ContactsInviteFingerprint : RemoteSettingFingerprint("optContactsInvite")
