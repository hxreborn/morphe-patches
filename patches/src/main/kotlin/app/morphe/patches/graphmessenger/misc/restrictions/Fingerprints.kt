/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

private const val MESSAGE_CLASS = "Lorg/telegram/tgnet/TLRPC\$Message;"
private const val CHAT_CLASS = "Lorg/telegram/tgnet/TLRPC\$Chat;"
private const val USER_FULL_CLASS = "Lorg/telegram/tgnet/TLRPC\$UserFull;"

private const val NO_FORWARDS_FIELD = "noforwards"
private const val PEER_NO_FORWARDS_FIELD = "noforwards_peer_enabled"

internal val messageNoForwardsRead =
    fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = MESSAGE_CLASS, name = NO_FORWARDS_FIELD)
internal val messageNoForwardsWrite =
    fieldAccess(opcode = Opcode.IPUT_BOOLEAN, definingClass = MESSAGE_CLASS, name = NO_FORWARDS_FIELD)
internal val chatNoForwardsRead =
    fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = CHAT_CLASS, name = NO_FORWARDS_FIELD)
internal val chatNoForwardsWrite =
    fieldAccess(opcode = Opcode.IPUT_BOOLEAN, definingClass = CHAT_CLASS, name = NO_FORWARDS_FIELD)
internal val peerNoForwardsRead =
    fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = USER_FULL_CLASS, name = PEER_NO_FORWARDS_FIELD)
internal val peerNoForwardsWrite =
    fieldAccess(opcode = Opcode.IPUT_BOOLEAN, definingClass = USER_FULL_CLASS, name = PEER_NO_FORWARDS_FIELD)

internal object MessageNoForwardsReadFingerprint : Fingerprint(filters = listOf(messageNoForwardsRead))

internal object ChatNoForwardsReadFingerprint : Fingerprint(filters = listOf(chatNoForwardsRead))

internal object PeerNoForwardsReadFingerprint : Fingerprint(filters = listOf(peerNoForwardsRead))
